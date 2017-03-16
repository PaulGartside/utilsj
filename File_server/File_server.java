
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

class File_server
{
  public static void main(String[] args)
  {
    if( args.length != 1 ) Usage();

    try {
      File_server me = new File_server( args );

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
    System.out.println("usage: File_server peer_ip_addr");
    System.exit( 0 );
  }
  void Msg( String msg )
  {
    System.out.println("File_server: " + msg );
  }
  void Die( String msg )
  {
    Msg( msg );
    System.exit( 0 );
  }
  File_server( String[] args ) throws Exception
  {
    m_peer_str  = args[0];
    m_socket    = new DatagramSocket( SERVER_PORT );
    m_peer_addr = InetAddress.getByName( m_peer_str );
  }
  void Run() throws Exception
  {
    // Keep running until killed:
    while( true )
    {
      Wait_4_Request();

      if( m_req_type == Request_Type.GET )
      {
        try {
          new Handle_Get( m_socket, m_peer_addr, m_peer_port, m_fname ).Run();
        }
        catch( FileNotFoundException e )
        {
          Send_Error( "File_server: File not found: "+ m_fname );
        }
      }
      else if( m_req_type == Request_Type.PUT )
      {
        final Path m_path = FileSystems.getDefault().getPath( m_fname );

        if( Files.isDirectory( m_path ) )
        {
          Send_Error( "File_server: File is a directory: "+ m_fname );
        }
        else if( Files.isRegularFile( m_path ) )
        {
          Send_Error( "File_server: File already exists: "+ m_fname );
        }
        else {
          new Handle_Put( m_socket, m_peer_addr, m_peer_port, m_fname ).Run();
        }
      }
    }
  }
  void Wait_4_Request() throws IOException, SocketException
  {
    boolean rcvd_req = false;

    Msg("Listening on port: "+ SERVER_PORT );

    while( !rcvd_req )
    {
      m_req_pkt.setLength( MAX_PKT_SIZE );
      m_socket.setSoTimeout( 0 );
      m_socket.receive( m_req_pkt );

      if( m_req_pkt.getAddress().equals( m_peer_addr ) )
      {
        final byte[] pkt_data = m_req_pkt.getData();
        final int    pkt_len  = m_req_pkt.getLength();

        if( 3 <= pkt_len )
        {
          final int OPCODE = pkt_data[0];

          if( OPCODE == OPCODE_GET_REQ
           || OPCODE == OPCODE_PUT_REQ )
          {
            if( OPCODE == OPCODE_GET_REQ ) m_req_type = Request_Type.GET;
            else                           m_req_type = Request_Type.PUT;

            rcvd_req = true;

            m_peer_port = m_req_pkt.getPort();

            m_sb.setLength( 0 );
            for( int k=1; k<pkt_len-1; k++ )
            {
              m_sb.append( (char)pkt_data[k] );
            }
            m_fname = m_sb.toString();
          }
        }
      }
    }
  }
  void Send_Error( String msg ) throws IOException
  {
    final int msg_len = msg.length();
    final int pkt_len = msg_len + 2;

    DatagramPacket err_pkt = new DatagramPacket( new byte[pkt_len], pkt_len );

    final byte[] pkt_data = err_pkt.getData();
    pkt_data[ 0 ] = OPCODE_ERROR;

    for( int k=0; k<msg_len; k++ )
    {
      pkt_data[ k+1 ] = (byte)msg.charAt( k );
    }
    pkt_data[ 1+msg.length() ] = 0;

    err_pkt.setAddress( m_peer_addr );
    err_pkt.setPort( m_peer_port );
    err_pkt.setData( pkt_data ); // May not be necessary
    m_socket.send( err_pkt );

    Msg( msg );
  }
  static final int  SERVER_PORT     = 6969;
  static final int  MAX_DATA_SIZE   = 512;
  static final int  MAX_PKT_SIZE    = MAX_DATA_SIZE + 5;
  static final byte OPCODE_GET_REQ  = 1;
  static final byte OPCODE_PUT_REQ  = 2;
  static final byte OPCODE_ERROR    = 5;

  final String         m_peer_str;
  final DatagramSocket m_socket;
  final InetAddress    m_peer_addr;
        int            m_peer_port;

  DatagramPacket m_req_pkt = new DatagramPacket( new byte[MAX_PKT_SIZE], MAX_PKT_SIZE );
  Request_Type   m_req_type = Request_Type.UNKNOWN;
  StringBuilder  m_sb = new StringBuilder();
  String         m_fname;
}

enum Request_Type
{
  UNKNOWN,
  GET,
  PUT
}

