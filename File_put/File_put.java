////////////////////////////////////////////////////////////////////////////////
// File Put Java Implementation                                               //
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
import java.io.FileInputStream;
import java.io.BufferedInputStream;
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
import java.util.Iterator;

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
//     |-- Write Request -->|
//     |                    |
//     |<-- Ack 0 ----------|
//     |                    |
//     |--- Data Packet 1 ->|
//     |<-- Ack 1 ----------|
//     |                    |
//     |--- Data Packet 2 ->|
//     |--- Data Packet 3 ->|
//     |<-- Ack 2 ----------|
//     |<-- Ack 3 ----------|
//     |                    |
//

class File_put
{
  public static void main(String[] args)
  {
    if( args.length < 2 || 3 < args.length ) Usage();

    try {
      File_put me = new File_put( args );

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
    System.out.println("usage: File_put server_ip_addr source_file [destination_file]");
    System.exit( 0 );
  }
  void Msg( String msg )
  {
    System.out.println("File_put: " + msg );
  }
  void Die( String msg )
  {
    Msg( msg );
    System.exit( 0 );
  }
  File_put( String[] args ) throws Exception
  {
    m_peer_str  = args[0];
    m_src_fname = args[1];
    m_dst_fname = args.length == 3 ? args[2] : m_src_fname;
    m_src_path  = FileSystems.getDefault().getPath( m_src_fname );

    if( Files.isDirectory( m_src_path ) )
    {
      Die( m_src_fname + " is a directory");
    }
    File infile = m_src_path.toFile();

    m_fis = new FileInputStream( infile );
    m_bis = new BufferedInputStream( m_fis, 512 );

    m_socket    = new DatagramSocket();
    m_peer_addr = InetAddress.getByName( m_peer_str );
  }
  void Run() throws Exception
  {
    if( !m_peer_addr.isReachable( 500 ) )
    {
      Die(m_peer_str + ": Un-reachable");
    }
    Send_Put_Request();

    Wait_4_Initial_Ack( 500 );

    while( m_running )
    {
      Send_Data();

      Recv_Ack();
    }
    m_fis.close();

    if( m_success )
    {
      Msg( "Sent to "
         + m_peer_addr.getHostAddress() +":"
         + SERVER_PORT +": "
         + m_src_fname );
      Msg( m_stats.toString() );
    }
  }
  void Send_Put_Request() throws IOException
  {
    final int PKT_LEN = m_dst_fname.length() + 2;

    DatagramPacket put_pkt = new DatagramPacket( new byte[PKT_LEN], PKT_LEN );

    final byte[] pkt_data = put_pkt.getData();
    pkt_data[ 0 ] = OPCODE_PUT_REQ;
    for( int k=0; k<m_dst_fname.length(); k++ )
    {
      pkt_data[ k+1 ] = (byte)m_dst_fname.charAt( k );
    }
    pkt_data[ PKT_LEN-1 ] = 0;

    put_pkt.setAddress( m_peer_addr );
    put_pkt.setPort( SERVER_PORT );
    put_pkt.setData( pkt_data ); // May not be necessary
    m_socket.send( put_pkt );
  }
  void Wait_4_Initial_Ack( final long TIMEOUT ) throws SocketException
                                                     , IOException
  {
    final long START_TIME = System.currentTimeMillis();

    for( long time_left = TIMEOUT
       ; 0 < time_left
       ; time_left = TIMEOUT - (System.currentTimeMillis() - START_TIME) )
    {
      m_socket.setSoTimeout( (int)time_left );
      m_socket.receive( m_ack_pkt );

      if( m_ack_pkt.getAddress().equals( m_peer_addr )
       && m_ack_pkt.getPort() == SERVER_PORT )
      {
        // Received a packet from TGT_ADDR:TGT_PORT
        if( !Received_Initial_Ack() )
        {
          m_running = false;
        }
        return;
      }
      else {
        ; // Packet not from target process, so wait for another packet
      }
    }
    m_running = false;
  }
  boolean Received_Initial_Ack()
  {
    final byte[] pkt_data = m_ack_pkt.getData();
    final int    pkt_len  = m_ack_pkt.getLength();

    if( ACK_SIZE <= pkt_len )
    {
      final int OPCODE = pkt_data[0];

      if( OPCODE == OPCODE_ACK )
      {
        final int pkt_num = ( pkt_data[1] << 24 & 0xFF000000 )
                          | ( pkt_data[2] << 16 & 0x00FF0000 ) 
                          | ( pkt_data[3] <<  8 & 0x0000FF00 ) 
                          | ( pkt_data[4] <<  0 & 0x000000FF );
        if( 0 == pkt_num )
        {
          return true;
        }
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
    return false;
  }
  void Send_Data() throws IOException
  {
    if( !m_sent_last_pkt && m_sent.size() < MAX_WINDOW_SIZE )
    {
      // Read from source file:
      int bytes_read = m_fis.read( m_bytes, 0, 512 );

      if( bytes_read < 512 )
      {
        m_sent_last_pkt = true;
        if( bytes_read<0 ) bytes_read = 0;
      }
      DatagramPacket data_pkt = Assemble_Pkt_2_Send( bytes_read );

      // Send packet:
      data_pkt.setAddress( m_peer_addr );
      data_pkt.setPort( SERVER_PORT );
      m_socket.send( data_pkt );

      m_sent.put( m_last_sent_pkt
                , new Sent_Pkt_Info( data_pkt, System.currentTimeMillis() ) );

      m_stats.Set_Window( m_sent.size() );
      m_stats.Inc_Pkts_Sent();
    }
  }
  DatagramPacket Assemble_Pkt_2_Send( final int bytes_read )
  {
    DatagramPacket
    data_pkt = new DatagramPacket( new byte[bytes_read+5], bytes_read+5 );

    m_last_sent_pkt++; // Set this to packet number being sent

    final byte[] pkt_data = data_pkt.getData();
    pkt_data[ 0 ] = OPCODE_DATA;
    pkt_data[ 1 ] = (byte)(m_last_sent_pkt >> 24 & 0xFF);
    pkt_data[ 2 ] = (byte)(m_last_sent_pkt >> 16 & 0xFF);
    pkt_data[ 3 ] = (byte)(m_last_sent_pkt >>  8 & 0xFF);
    pkt_data[ 4 ] = (byte)(m_last_sent_pkt >>  0 & 0xFF);

    for( int k=0; k<bytes_read; k++ )
    {
      pkt_data[ k+5 ] = m_bytes[k];
    }
    data_pkt.setData( pkt_data ); // May not be necessary

    return data_pkt;
  }
  void Recv_Ack() throws IOException, SocketException
  {
    if( m_sent.size() < MAX_WINDOW_SIZE )
    {
      if( Socket_Has_Data() )
      {
        Handle_Ack();
      }
    }
    else {
      m_socket.setSoTimeout( 500 );
      m_socket.receive( m_ack_pkt );

      if( m_ack_pkt.getAddress().equals( m_peer_addr )
       && m_ack_pkt.getPort() == SERVER_PORT )
      {
        Handle_Ack();
      }
    }
    // Resend data packets for which we have not received an ack:
    Iterator<Sent_Pkt_Info> I = m_sent.values().iterator();
    while( I.hasNext() )
    {
      final long time_ms = System.currentTimeMillis();

      Sent_Pkt_Info pkt_info = I.next();
      if( 500 < time_ms - pkt_info.m_time )
      {
        // Have not received an ack, so resend data:
        m_socket.send( pkt_info.m_pkt );
        m_stats.Inc_Resend();
        pkt_info.m_time = time_ms;
      }
    }
  }
  boolean Socket_Has_Data() throws IOException, SocketException
  {
    try {
      m_socket.setSoTimeout( 1 );
      m_socket.receive( m_ack_pkt );

      if( m_ack_pkt.getAddress().equals( m_peer_addr )
       && m_ack_pkt.getPort() == SERVER_PORT )
      {
        return true;
      }
    }
    catch( SocketTimeoutException e )
    {
    }
    return false;
  }
  void Handle_Ack()
  {
    if( m_ack_pkt.getAddress().equals( m_peer_addr ) )
    {
      final byte[] pkt_data = m_ack_pkt.getData();
      final int    pkt_len  = m_ack_pkt.getLength();

      if( ACK_SIZE == pkt_len )
      {
        final int OPCODE = pkt_data[0];

        if( OPCODE == OPCODE_ACK )
        {
          final int pkt_num = ( pkt_data[1] << 24 & 0xFF000000 )
                            | ( pkt_data[2] << 16 & 0x00FF0000 ) 
                            | ( pkt_data[3] <<  8 & 0x0000FF00 ) 
                            | ( pkt_data[4] <<  0 & 0x000000FF );
          if( m_sent.containsKey( pkt_num ) )
          {
            m_sent.remove( pkt_num );

            if( m_sent_last_pkt && 0==m_sent.size() )
            {
              // Sent all packets and received all acks
              m_running = false;
              m_success = true;
            }
          }
        }
      }
    }
  }
  static final int  SERVER_PORT    = 6969;
  static final int  MAX_DATA_SIZE  = 512;
  static final int  MAX_PKT_SIZE   = MAX_DATA_SIZE + 5;
  static final int  MAX_WINDOW_SIZE = 8;
  static final int  ACK_SIZE       = 5;
  static final byte OPCODE_GET_REQ = 1;
  static final byte OPCODE_PUT_REQ = 2;
  static final byte OPCODE_DATA    = 3;
  static final byte OPCODE_ACK     = 4;
  static final byte OPCODE_ERROR   = 5;

  final String   m_peer_str;
  final String   m_src_fname;
  final String   m_dst_fname;
  final Path     m_src_path;
  final DatagramSocket m_socket;
  final InetAddress    m_peer_addr;

  final FileInputStream     m_fis;
  final BufferedInputStream m_bis;

  // m_ack_pkt stores received packets, to okay to use MAX_PKT_SIZE
  DatagramPacket m_ack_pkt = new DatagramPacket( new byte[MAX_PKT_SIZE], MAX_PKT_SIZE );
  byte[]         m_bytes = new byte[512];

  boolean m_success       = false;
  boolean m_running       = true;
  boolean m_sent_last_pkt = false;
  int     m_last_sent_pkt = 0; // Packet numbers start at 1
  Stats   m_stats         = new Stats();

  HashMap<Integer,Sent_Pkt_Info> m_sent = new HashMap<>();
}

class Sent_Pkt_Info
{
  Sent_Pkt_Info( DatagramPacket pkt, long time )
  {
    m_pkt  = pkt;
    m_time = time;
  }
  DatagramPacket m_pkt;
  long           m_time; 
}

class Stats
{
  int m_max_window  = 0;
  int m_num_resends = 0;
  int m_pkts_sent   = 0;

  void Set_Window( final int window_size )
  {
    m_max_window = Math.max( window_size, m_max_window );
  }
  void Inc_Resend()
  {
    m_num_resends++;
  }
  void Inc_Pkts_Sent()
  {
    m_pkts_sent++;
  }
  public String toString()
  {
    return "Max Window="+ m_max_window
         + ", Num Resends="+ m_num_resends
         + ", Pkts Sent="+ m_pkts_sent;
  }
}

