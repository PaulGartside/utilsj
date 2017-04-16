////////////////////////////////////////////////////////////////////////////////
// File Server Java Implementation                                            //
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
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

class Handle_Put
{
  Handle_Put( DatagramSocket socket
            , InetAddress    peer_addr
            , int            peer_port
            , String         in_fname )
  {
    m_socket     = socket;
    m_peer_addr  = peer_addr;
    m_peer_port  = peer_port;
    m_out_fname  = in_fname;

    m_ack_pkt.setAddress( m_peer_addr );
    m_ack_pkt.setPort( m_peer_port );
  }
  void Msg( String msg )
  {
    System.out.println("File_server: " + msg );
  }
  void Run() throws FileNotFoundException, IOException
  {
    m_fos = new FileOutputStream( m_out_fname );
    m_bos = new BufferedOutputStream( m_fos, 512 );

    // Send first ack to get the peer started sending data:
    Send_Ack( m_rcvd_pkt_num );

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
         + m_peer_port +": "
         + m_out_fname );
    }
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
       && m_data_pkt.getPort() == m_peer_port )
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
    }
  }
  static final int  MAX_WINDOW_SIZE = 8;
  static final int  MAX_DATA_SIZE   = 512;
  static final int  MAX_PKT_SIZE    = MAX_DATA_SIZE + 5;
  static final int  ACK_SIZE        = 5;
  static final byte OPCODE_DATA     = 3;
  static final byte OPCODE_ACK      = 4;

  FileOutputStream     m_fos;
  BufferedOutputStream m_bos;

  final DatagramSocket m_socket;
  final InetAddress    m_peer_addr;
  final int            m_peer_port;
  final String         m_out_fname;

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

