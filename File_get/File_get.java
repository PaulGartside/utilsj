////////////////////////////////////////////////////////////////////////////////
// File Get Java Implementation                                               //
// Copyright (c) 16 Apr 2017 Paul J. Gartside                                 //
////////////////////////////////////////////////////////////////////////////////
// Permission is hereby granted, free of charge, to any person obtaining a    //
// copy of this software and associated documentation files (the "Software"), //
// to deal in the Software without restriction, including without  limitation //
// the rights to use, copy, modify, merge, publish, distribute, sublicense,   //
// and/or sell copies of the Software, and to permit persons to whom the      //
// Software is furnished to do so, subject to the following conditions:       //
//                                                                            //
// The above copyright notice and this permission notice shall be included in //
// all copies or substantial portions of the Software.                        //
//                                                                            //
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR //
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,   //
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL    //
// THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER //
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING    //
// FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER        //
// DEALINGS IN THE SOFTWARE.                                                  //
////////////////////////////////////////////////////////////////////////////////

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

// ---------------------
// File Protocol Summary
// ---------------------
//
//               --------------------
// Read  Request | 1 | filename | 0 |
// GET           --------------------
// 
//               --------------------
// Write Request | 2 | filename | 0 |
// PUT           --------------------
// 
//                   |< 4 bytes >| 0 to 512 bytes|
//               ---------------------------------
// Data Packet   | 3 | packetnum | data          |
// DATA          ---------------------------------
// 
//                   |< 4 bytes >|
//               -----------------
// Acknowledge   | 4 | packetnum |
// ACK           -----------------
// 
//               ---------------------
// Error         | 5 | errstring | 0 |
// ERROR         ---------------------
//
// ----------           ----------
// | Client |           | Server |
// ----------           ----------
//     |                    |
//     |--- Read Request -->|
//     |                    |
//     |<-- Data Packet 1 --|
//     |--- Ack 1 --------->|
//     |                    |
//     |<-- Data Packet 2 --|
//     |<-- Data Packet 3 --|
//     |--- Ack 2 --------->|
//     |--- Ack 3 --------->|
//     |                    |
//

class File_get
{
  public static void main(String[] args)
  {
    if( args.length < 2 || 3 < args.length ) Usage();

    try {
      File_get me = new File_get( args );

      me.Run();
    }
    catch( Exception e )
    {
      Handle_Exception( e );
    }
  }
  static void Handle_Exception( Exception e )
  {
    e.printStackTrace( System.err );
    System.exit( 0 );
  }
  static void Usage()
  {
    System.out.println("usage: File_get server_ip_addr source_file [destination_file]");
    System.exit( 0 );
  }
  void Msg( String msg )
  {
    System.out.println("File_get: " + msg );
  }
  void Die( String msg )
  {
    Msg( msg );
    System.exit( 0 );
  }
  File_get( String[] args ) throws Exception
  {
    m_peer_str  = args[0];
    m_src_fname = args[1];
    m_dst_fname = args.length == 3 ? args[2] : m_src_fname;
    m_dst_path  = FileSystems.getDefault().getPath( m_dst_fname );

    if( Files.isDirectory( m_dst_path ) )
    {
      Die( m_dst_fname + " is a directory");
    }
    File outfile = m_dst_path.toFile();

    m_fos = new FileOutputStream( outfile );
    m_bos = new BufferedOutputStream( m_fos, 512 );

    m_socket    = new DatagramSocket();
    m_peer_addr = InetAddress.getByName( m_peer_str );
  }
  void Run() throws Exception
  {
    if( !m_peer_addr.isReachable( 500 ) )
    {
      Die(m_peer_str + ": Un-reachable");
    }
    m_ack_pkt.setAddress( m_peer_addr );
    m_ack_pkt.setPort( SERVER_PORT );

    Send_Get_Request();

    while( m_running )
    {
      Recv_Data();
    }
    m_bos.flush();
    m_fos.close();

    if( m_success )
    {
      Msg( "Received from "
         + m_peer_addr.getHostAddress() +":"
         + SERVER_PORT +": "
         + m_dst_fname );
    }
  }
  void Send_Get_Request() throws IOException
  {
    final int PKT_LEN = m_src_fname.length() + 2;

    DatagramPacket get_pkt = new DatagramPacket( new byte[PKT_LEN], PKT_LEN );

    final byte[] pkt_data = get_pkt.getData();
    pkt_data[ 0 ] = OPCODE_GET_REQ;
    for( int k=0; k<m_src_fname.length(); k++ )
    {
      pkt_data[ k+1 ] = (byte)m_src_fname.charAt( k );
    }
    pkt_data[ PKT_LEN-1 ] = 0;

    get_pkt.setAddress( m_peer_addr );
    get_pkt.setPort( SERVER_PORT );
    get_pkt.setData( pkt_data ); // May not be necessary
    m_socket.send( get_pkt );
  }
  void Recv_Data() throws SocketException, IOException
  {
    Wait_4_Data_Packet( 500 );

    if( m_running )
    {
      Get_Packet_Info();

      if( m_pkt_is_data )
      {
        final int max_pkt_num = m_next_pkt_num + MAX_WINDOW_SIZE - 1;

        if( m_next_pkt_num <= m_rcvd_pkt_num && m_rcvd_pkt_num <= max_pkt_num )
        {
          if( m_next_pkt_num < m_rcvd_pkt_num )
          {
            // Received a packet out of order, so cache it:
            m_data.put( m_rcvd_pkt_num, m_data_pkt );
          }
          else { // Received next packet, to save to file:
            Save_Packet_2_File( m_data_pkt );

            // See how many other previously cached packets can be saved to file:
            while( m_data.containsKey( m_next_pkt_num ) )
            {
              Save_Packet_2_File( m_data.remove( m_next_pkt_num ) );
            }
            if( m_got_last_pkt && 0==m_data.size() )
            {
              m_running = false;
              m_success = true;
            }
          }
          // Always ack a received packet:
          Send_Ack( m_rcvd_pkt_num );
        }
      }
    }
  }
  void Save_Packet_2_File( DatagramPacket pkt ) throws IOException
  {
    final int data_len = pkt.getLength()-5;

    if( data_len < 512 ) m_got_last_pkt = true;

    m_bos.write( pkt.getData(), 5, data_len );
    m_next_pkt_num++;
  }
  void Send_Ack( final int ack_num ) throws IOException
  {
    final byte[] pkt_data = m_ack_pkt.getData();
    pkt_data[ 0 ] = OPCODE_ACK;
    pkt_data[ 1 ] = (byte)(ack_num >> 24 & 0xFF);
    pkt_data[ 2 ] = (byte)(ack_num >> 16 & 0xFF);
    pkt_data[ 3 ] = (byte)(ack_num >>  8 & 0xFF);
    pkt_data[ 4 ] = (byte)(ack_num >>  0 & 0xFF);
    m_ack_pkt.setData( pkt_data ); // May not be necessary
    m_socket.send( m_ack_pkt );
  }
  void
  Wait_4_Data_Packet( final long TIMEOUT ) throws SocketException
                                                , IOException
  {
    final long START_TIME = System.currentTimeMillis();

    for( long time_left = TIMEOUT
       ; 0 < time_left
       ; time_left = TIMEOUT - (System.currentTimeMillis() - START_TIME) )
    {
      m_data_pkt.setLength( MAX_PKT_SIZE );
      m_socket.setSoTimeout( (int)time_left );
      m_socket.receive( m_data_pkt );

      if( m_data_pkt.getAddress().equals( m_peer_addr )
       && m_data_pkt.getPort() == SERVER_PORT )
      {
        return; // Received a packet from TGT_ADDR:TGT_PORT
      }
      else {
        ; // Packet not from target process, so wait for another packet
      }
    }
    m_running = false;
  }
  void Get_Packet_Info()
  {
    m_pkt_is_data  = false;
    m_rcvd_pkt_num = 0;

    final byte[] pkt_data = m_data_pkt.getData();
    final int    pkt_len  = m_data_pkt.getLength();

    if( 5 <= pkt_len )
    {
      final int OPCODE = pkt_data[0];

      if( OPCODE == OPCODE_DATA )
      {
        m_pkt_is_data = true;
        m_rcvd_pkt_num = ( pkt_data[1] << 24 & 0xFF000000 )
                       | ( pkt_data[2] << 16 & 0x00FF0000 ) 
                       | ( pkt_data[3] <<  8 & 0x0000FF00 ) 
                       | ( pkt_data[4] <<  0 & 0x000000FF );
      }
      else if( OPCODE == OPCODE_ERROR )
      {
        StringBuilder sb = new StringBuilder( pkt_len-2 );

        for( int k=1; k<pkt_len-1; k++ )
        {
          sb.append( (char)pkt_data[k] );
        }
        Msg( sb.toString() );
        m_running = false;
      }
    }
  }
  static final int  SERVER_PORT     = 6969;
  static final int  MAX_WINDOW_SIZE = 8;
  static final int  MAX_DATA_SIZE   = 512;
  static final int  MAX_PKT_SIZE    = MAX_DATA_SIZE + 5;
  static final int  ACK_SIZE        = 5;
  static final byte OPCODE_GET_REQ  = 1;
  static final byte OPCODE_PUT_REQ  = 2;
  static final byte OPCODE_DATA     = 3;
  static final byte OPCODE_ACK      = 4;
  static final byte OPCODE_ERROR    = 5;

  final String   m_peer_str;
  final String   m_src_fname;
  final String   m_dst_fname;
  final Path     m_dst_path;
  final FileOutputStream     m_fos;
  final BufferedOutputStream m_bos;

  final DatagramSocket m_socket;
  final InetAddress    m_peer_addr;

  // m_data_pkt receives data packets, to okay to use MAX_PKT_SIZE
  DatagramPacket m_data_pkt = new DatagramPacket( new byte[MAX_PKT_SIZE], MAX_PKT_SIZE );
  DatagramPacket m_ack_pkt  = new DatagramPacket( new byte[ACK_SIZE], ACK_SIZE );

  boolean m_success      = false;
  boolean m_running      = true;
  boolean m_got_last_pkt = false;
  int     m_rcvd_pkt_num = 0;
  int     m_next_pkt_num = 1; // Next expected packet number
  boolean m_pkt_is_data  = false;

  HashMap<Integer,DatagramPacket> m_data = new HashMap<>();
}

