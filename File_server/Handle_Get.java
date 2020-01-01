////////////////////////////////////////////////////////////////////////////////
// File Server Java Implementation                                            //
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
//

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;

class Handle_Get
{
  Handle_Get( Socket          socket
            , DataInputStream din_stream )
  {
    m_socket         = socket;
    m_din_stream     = din_stream;
    m_client_IP_addr = m_socket.getInetAddress();
  }
  void Msg( String msg )
  {
    System.out.println("File_server: " + msg );
  }
  void Die( String msg )
  {
    Msg( msg );

    m_running = false;
  }
  void Run()
  {
    if( m_running )
    {
      final boolean ok = Receive_Read_Request();
      Send_Read_Response( ok );
      Send_File_Data();

      if( m_running )
      {
        Msg( "Sent to "
           + m_client_IP_addr.getHostAddress() +":"
           + m_socket.getPort() +": "
           + m_src_fname );
      }
    }
  }
  boolean Receive_Read_Request()
  {
    boolean ok = false;

    try {
      ok = Receive_Read_Request_e();
    }
    catch( IOException e )
    {
      Die("Receive_Read_Request(): IOException: "+ e);
    }
    return ok;
  }
  //               |<1>|<-- 4 bytes ---->|
  //               ----------------------------------------
  // Read  Request | 1 | Filename length | Filename       |
  // GET           |   | Num utf16 chars | in utf16 chars |
  //               ----------------------------------------
  boolean Receive_Read_Request_e() throws IOException
  {
    boolean ok = false;

    final int fname_len = m_din_stream.readInt();

    if( fname_len <= 0 )
    {
      m_err_msg = m_socket.getLocalSocketAddress()
                + ": Bad filename length: "+ fname_len;
    }
    else {
      StringBuilder sb = new StringBuilder( fname_len );

      for( int k=0; k<fname_len; k++ )
      {
        sb.append( m_din_stream.readChar() );
      }
      m_src_fname = sb.toString();

      m_src_path = FileSystems.getDefault().getPath( m_src_fname );

      if( Files.isDirectory( m_src_path ) )
      {
        m_err_msg = m_socket.getLocalSocketAddress()
                  + ": File is a directory: "+ m_src_fname;
      }
      else if( ! Files.isRegularFile( m_src_path ) )
      {
        m_err_msg = m_socket.getLocalSocketAddress()
                  + ": File does not exist or is not regular file: "+ m_src_fname;
      }
      else {
        m_src_file = m_src_path.toFile();

        if( Integer.MAX_VALUE < m_src_file.length() )
        {
          m_err_msg = m_socket.getLocalSocketAddress()
                    + ": File too large, not sending: "+ m_src_fname;
        }
        else {
          ok = true;
        }
      }
    }
    return ok;
  }
  void Send_Read_Response( final boolean ok )
  {
    try {
      Send_Read_Response_e( ok );
    }
    catch( IOException e )
    {
      Die("Send_Read_Response(): IOException: "+ e);
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
  void Send_Read_Response_e( final boolean ok ) throws IOException
  {
    Get_Out_Stream();

    if( m_running )
    {
      m_dout_stream.writeByte( OPCODE_GET_RESP );

      if( ok ) {
        m_dout_stream.writeByte( 1 );
        m_dout_stream.writeInt( (int)m_src_file.length() );
      }
      else {
        m_dout_stream.writeByte( 0 );
        m_dout_stream.writeInt( m_err_msg.length() );
        m_dout_stream.writeChars( m_err_msg );
        Die( m_err_msg );
      }
    }
  }

  void Send_File_Data()
  {
    try {
      Send_File_Data_e();
    }
    catch( FileNotFoundException e )
    {
      Die("Send_File_Data(): FileNotFoundException: "+ e);
    }
    catch( IOException e )
    {
      Die("Send_File_Data(): IOException: "+ e);
    }
  }
  //               |<1>|
  // Read/Write    -----------------
  // DATA          | 5 | File data |
  //               -----------------
  void Send_File_Data_e() throws IOException, FileNotFoundException
  {
    if( m_running )
    {
      m_dout_stream.writeByte( OPCODE_DATA );

      FileInputStream fis = new FileInputStream( m_src_file );

      long total_bytes_read = 0;

      while( total_bytes_read < m_src_file.length() )
      {
        // Read from source file:
        final int bytes_read = fis.read( m_bytes, 0, 512 );

        if( 0 < bytes_read )
        {
          total_bytes_read += bytes_read;

          m_dout_stream.write( m_bytes, 0, bytes_read );
        }
      }
      fis.close();
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
      Die("m_socket.getOutputStream(): IOException: " + e);
    }
  }
  static final byte OPCODE_GET_REQ  = 1;
  static final byte OPCODE_GET_RESP = 2;
  static final byte OPCODE_PUT_REQ  = 3;
  static final byte OPCODE_PUT_RESP = 4;
  static final byte OPCODE_DATA     = 5;

  final Socket          m_socket;
  final DataInputStream m_din_stream;
  final InetAddress     m_client_IP_addr;
  final byte[]          m_bytes = new byte[512];

  boolean          m_running = true;
  String           m_src_fname;
  Path             m_src_path;
  File             m_src_file;
  DataOutputStream m_dout_stream;
  String           m_err_msg;
}

