////////////////////////////////////////////////////////////////////////////////
// File Get Java Implementation                                               //
// Copyright (c) 31 Dec 2019 Paul J. Gartside                                 //
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
// ---------------------
// File Protocol Summary
// ---------------------
//
//               |<1>|<-- 4 bytes ---->|
//               ----------------------------------------
// Read  Request | 1 | Filename length | Filename       |
// GET           |   | Num utf16 chars | in utf16 chars |
//               ----------------------------------------
//
//               |<1>|< 1 byte >|<-- 4 bytes ------>|
// Read          ------------------------------------
// Response      | 2 | True=1   | num_bytes in file |
// Affirmative   ------------------------------------
//
//               |<1>|< 1 byte >|<-- 4 bytes ---->|
// Read          ----------------------------------------------
// Response      | 2 | False=0  | Num utf16 chars | errstring |
// Negative      |   | 00000000 | in errstring    |           |
//               ----------------------------------------------
//
//               |<1>|< 4 bytes >|<-- 4 bytes ---->|
//               ----------------------------------------------------
// Write Request | 3 | num_bytes | Filename length | Filename       |
// PUT           |   | in file   | Num utf16 chars | in utf16 chars |
//               ----------------------------------------------------
//
//               |<1>|< 1 byte >|
// Write         ----------------
// Response      | 4 | True=1   |
// Affirmative   |   | 00000001 |
// Affirmative   ----------------
//
//               |<1>|< 1 byte >|<-- 4 bytes ---->|
// Write         ----------------------------------------------
// Response      | 4 | False=0  | Num utf16 chars | errstring |
// Negative      |   | 00000000 | in errstring    |           |
//               ----------------------------------------------
//
//               |<1>|
// Read/Write    -----------------
// DATA          | 5 | File data |
//               -----------------
//
// ----------                         ----------
// | Client |                         | Server |
// ----------                         ----------
//     |                                  |
//     |--- Read Request ---------------->|
//     |<-- Read Response Affirmative ----|
//     |<-- File data --------------------|
//     |                                  |
//     |--- Read Request ---------------->|
//     |<-- Read Response Negative -------|
//     |                                  |
//     |--- Write Request --------------->|
//     |<-- Write Response Affirmative ---|
//     |--- File data ------------------->|
//     |                                  |
//     |--- Write Request --------------->|
//     |<-- Write Response Negative ------|
//     |                                  |

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.StringBuilder;
import java.net.Socket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.channels.IllegalBlockingModeException;

class File_get
{
  public static void main(String[] args)
  {
    if( args.length < 2 || 3 < args.length ) Usage();

    try {
      new File_get( args ).Run();
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

    m_running = false;
  }
  File_get( String[] args )
  {
    m_server_str = args[0];
    m_src_fname  = args[1];
    m_dst_fname  = args.length == 3 ? args[2] : m_src_fname;
    m_dst_path   = FileSystems.getDefault().getPath( m_dst_fname );

    m_socket = new Socket();
  }
  void Run()
  {
    Check_Destination_File();
    Check_Server_Reachable();
    Connect();
    Get_In_Stream();
    Get_Out_Stream();
    Send_Read_Request();
    final long bytes_in_file = Receive_Read_Response();
    Receive_File_Data( bytes_in_file );
    Print_summary_message( bytes_in_file );

    Clean_Up();
  }
  void Check_Destination_File()
  {
    if( Files.isDirectory( m_dst_path ) )
    {
      Die( m_dst_fname + " is a directory");
    }
    if( m_running )
    {
      m_dst_file = m_dst_path.toFile();
    }
  }
  void Check_Server_Reachable()
  {
    if( m_running )
    try {
      m_server_inet_addr = InetAddress.getByName( m_server_str );
      m_server_inet_sock_addr = new InetSocketAddress( m_server_inet_addr
                                                     , SERVER_PORT );
      if( !m_server_inet_addr.isReachable( IS_REACHABLE_TIMEOUT_MS ) )
      {
        Die(m_server_str + ": Un-reachable");
      }
    }
    catch( UnknownHostException e )
    {
      Die(m_server_str + ": UnknownHostException: "+ e);
    }
    catch( IOException e )
    {
      Die("Check_Server_Reachable(): IOException: "+ e);
    }
  }
  void Connect()
  {
    if( m_running )
    try {
      m_socket.connect( m_server_inet_sock_addr, CONNECT_TIMEOUT_MS );
    }
    catch( SocketTimeoutException e )
    {
      Die(m_server_str + ": SocketTimeoutException: " + e);
    }
    catch( IOException e )
    {
      Die("Connect(): IOException: " + e);
    }
    catch( IllegalBlockingModeException e )
    {
      Die("Connect(): IllegalBlockingModeException: " + e);
    }
    catch( IllegalArgumentException e )
    {
      Die("Connect(): IllegalArgumentException: " + e);
    }
    catch( Exception e )
    {
      Die("Connect(): Exception: " + e);
    }
  }

  void Get_In_Stream()
  {
    if( m_running )
    try {
      InputStream in_stream = m_socket.getInputStream();

      m_din_stream = new DataInputStream( in_stream );
    }
    catch( IOException e )
    {
      Die(m_server_str + ": m_socket.getInputStream(): IOException: " + e);
    }
  }
  void Get_Out_Stream()
  {
    if( m_running )
    try {
      OutputStream out_stream = m_socket.getOutputStream();

      m_dout_stream = new DataOutputStream( out_stream );
    }
    catch( IOException e )
    {
      Die(m_server_str + ": m_socket.getOutputStream(): IOException: " + e);
    }
  }

  //               |<1>|<-- 4 bytes ---->|
  //               ----------------------------------------
  // Read  Request | 1 | Filename length | Filename       |
  // GET           |   | Num utf16 chars | in utf16 chars |
  //               ----------------------------------------
  void Send_Read_Request()
  {
    if( m_running )
    try {
      m_dout_stream.writeByte( OPCODE_GET_REQ );
      m_dout_stream.writeInt( m_src_fname.length() );
      m_dout_stream.writeChars( m_src_fname );
      m_dout_stream.flush();
    }
    catch( IOException e )
    {
      Die("Send_Read_Request(): IOException: " + e);
    }
  }

  //               |<1>|< 1 byte >|<-- 4 bytes ------>|
  // Read          ------------------------------------
  // Response      | 2 | True=1   | num_bytes in file |
  // Affirmative   ------------------------------------
  //
  //               |<1>|< 1 byte >|<-- 4 bytes ---->|
  // Read          ----------------------------------------------
  // Response      | 2 | False=0  | Num utf16 chars | errstring |
  // Negative      |   | 00000000 | in errstring    |           |
  //               ----------------------------------------------
  long Receive_Read_Response()
  {
    long bytes_in_file = 0;

    if( m_running )
    try {
      final byte resp_OPCODE = m_din_stream.readByte();
      if( resp_OPCODE != OPCODE_GET_RESP )
      {
        Die( m_server_str +": expected read response OPCODE "+ OPCODE_GET_RESP
                          +" but received "+ resp_OPCODE );
      }
      final byte response = m_din_stream.readByte();
      if( response != 0 )
      {
        bytes_in_file = m_din_stream.readInt();
      }
      else {
        final int err_str_len = m_din_stream.readInt();
        StringBuilder sb = new StringBuilder( err_str_len );
        for( int k=0; k<err_str_len; k++ )
        {
          sb.append( m_din_stream.readChar() );
        }
        Die( m_server_str +": "+ sb.toString() );
      }
    }
    catch( IOException e )
    {
      Die("Receive_Read_Request(): IOException: " + e);
    }
    return bytes_in_file;
  }

  //               |<1>|
  // Read/Write    -----------------
  // DATA          | 5 | File data |
  //               -----------------
  void Receive_File_Data( final long bytes_in_file )
  {
    if( m_running )
    try {
      final byte op_code = m_din_stream.readByte();

      if( op_code != OPCODE_DATA )
      {
        Die("Receive_File_Data(): Received bad OPCODE: " + op_code);
      }
      else {
        FileOutputStream fos = new FileOutputStream( m_dst_file );
        BufferedOutputStream bos = new BufferedOutputStream( fos, 512 );

        final long st_time = System.currentTimeMillis();

        byte[] ba = new byte[512];

        long total_bytes_read = 0;

        while( total_bytes_read < bytes_in_file )
        {
          final int bytes_read = m_din_stream.read( ba );

          if( 0 < bytes_read )
          {
            total_bytes_read += bytes_read;

            bos.write( ba, 0, bytes_read );
          }
        }
        m_tranfer_time_ms = System.currentTimeMillis() - st_time;
        bos.flush();
        bos.close();
        fos.close();
      }
    }
    catch( FileNotFoundException e )
    {
      Die("Receive_File_Data(): FileNotFoundException: " + e);
    }
    catch( IOException e )
    {
      Die("Receive_File_Data(): IOException: " + e);
    }
  }
  void Print_summary_message( final long bytes_in_file )
  {
    if( m_running )
    {
      // If we are still running at this point we were successful,
      // so print summary message:
      String msg = "Received from "
                 + m_server_inet_addr.getHostAddress() +":"
                 + SERVER_PORT +": "
                 + m_dst_fname +", "
                 + bytes_in_file +" bytes in "
                 + m_tranfer_time_ms +" ms";

      if( 0 < m_tranfer_time_ms )
      {
        final double bits = bytes_in_file*8;
        final double seconds = ((double)m_tranfer_time_ms)/1000;
        long bits_per_sec = (long)(bits/seconds + 0.5);

        String rate_label = "bits/sec";

        if( 1e9 < bits_per_sec )
        {
          rate_label = "G-bits/sec";
          bits_per_sec = (long)((double)(bits_per_sec)/1e9 + 0.5);
        }
        else if( 1e6 < bits_per_sec )
        {
          rate_label = "M-bits/sec";
          bits_per_sec = (long)((double)(bits_per_sec)/1e6 + 0.5);
        }
        else if( 1e3 < bits_per_sec )
        {
          rate_label = "K-bits/sec";
          bits_per_sec = (long)((double)(bits_per_sec)/1e3 + 0.5);
        }
        msg += ", "+ bits_per_sec + " " + rate_label;
      }
      Msg( msg );
    }
  }
  void Clean_Up()
  {
    try {
      if( null != m_socket )
      {
        m_socket.close();
      }
      if( null != m_din_stream )
      {
        m_din_stream.close();
      }
      if( null != m_dout_stream )
      {
        m_dout_stream.close();
      }
    }
    catch( IOException e )
    {
      Msg("Clean_Up(): IOException: " + e);
    }
  }
  static final byte OPCODE_GET_REQ  = 1;
  static final byte OPCODE_GET_RESP = 2;
  static final byte OPCODE_PUT_REQ  = 3;
  static final byte OPCODE_PUT_RESP = 4;
  static final byte OPCODE_DATA     = 5;

  static final int SERVER_PORT             = 6969;
  static final int IS_REACHABLE_TIMEOUT_MS = 1000;
  static final int CONNECT_TIMEOUT_MS      = 1000;

  final String m_server_str;
  final String m_src_fname;
  final String m_dst_fname;
  final Path   m_dst_path;
  final Socket m_socket;

  boolean              m_running = true;
  InetAddress          m_server_inet_addr;
  InetSocketAddress    m_server_inet_sock_addr;
  File                 m_dst_file;
  BufferedOutputStream m_bos;
  DataInputStream      m_din_stream;
  DataOutputStream     m_dout_stream;
  long                 m_tranfer_time_ms;
}

