
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
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

class Handle_Get
{
  Handle_Get( DatagramSocket socket
            , InetAddress    peer_addr
            , int            peer_port
            , String         in_fname ) throws Exception
  {
    m_socket    = socket;
    m_peer_addr = peer_addr;
    m_peer_port = peer_port;
    m_in_fname  = in_fname;
    m_fis = new FileInputStream( m_in_fname );
    m_bis = new BufferedInputStream( m_fis, 512 );
  }
  void Msg( String msg )
  {
    System.out.println("File_server: " + msg );
  }
  void Run() throws FileNotFoundException, IOException
  {
    Send_Data();

    while( m_running )
    {
      Recv_Ack();

      Send_Data();
    }
    m_fis.close();

    if( m_success )
    {
      Msg( "Sent to "
         + m_peer_addr.getHostAddress() +":"
         + m_peer_port +": "
         + m_in_fname );
      Msg( m_stats.toString() );
    }
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
      data_pkt.setPort( m_peer_port );
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
       && m_ack_pkt.getPort() == m_peer_port )
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
       && m_ack_pkt.getPort() == m_peer_port )
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
  static final int  MAX_WINDOW_SIZE = 8;
  static final byte OPCODE_DATA     = 3;
  static final int  ACK_SIZE        = 5;
  static final byte OPCODE_ACK      = 4;

  final DatagramSocket m_socket;
  final InetAddress    m_peer_addr;
  final int            m_peer_port;
  final String         m_in_fname;

  final FileInputStream     m_fis;
  final BufferedInputStream m_bis;

  DatagramPacket m_ack_pkt = new DatagramPacket( new byte[ACK_SIZE], ACK_SIZE );
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

