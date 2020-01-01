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

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;

class Handle_Put
{
  Handle_Put( Socket          socket
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
    final boolean ok = Receive_Write_Request();
    Send_Write_Response( ok );
    Receive_File_Data();

    if( m_running )
    {
      Msg( "Received from "
         + m_client_IP_addr.getHostAddress() +":"
         + m_socket.getPort() +": "
         + m_dst_fname );
    }
  }
  boolean Receive_Write_Request()
  {
    boolean ok = false;

    try {
      ok = Receive_Write_Request_e();
    }
    catch( IOException e )
    {
      Die("Receive_Write_Request(): IOException: "+ e);
    }
    return ok;
  }
  //               |<1>|< 4 bytes >|<-- 4 bytes ---->|
  //               ----------------------------------------------------
  // Write Request | 3 | num_bytes | Filename length | Filename       |
  // PUT           |   | in file   | Num utf16 chars | in utf16 chars |
  //               ----------------------------------------------------
  boolean Receive_Write_Request_e() throws IOException
  {
    boolean ok = false;

    m_dst_file_len = m_din_stream.readInt();

    final int fname_len = m_din_stream.readInt();

    if( fname_len <= 0 )
    {
      m_err_msg = m_socket.getLocalSocketAddress()
                + ": Received bad filename length: "+ fname_len;
    }
    else {
      StringBuilder sb = new StringBuilder( fname_len );

      for( int k=0; k<fname_len; k++ )
      {
        sb.append( m_din_stream.readChar() );
      }
      m_dst_fname = sb.toString();

      m_dst_path = FileSystems.getDefault().getPath( m_dst_fname );

      if( Files.isDirectory( m_dst_path ) )
      {
        m_err_msg = m_socket.getLocalSocketAddress()
                  + ": File is a directory: "+ m_dst_fname;
      }
      else if( Files.isRegularFile( m_dst_path ) )
      {
        m_err_msg = m_socket.getLocalSocketAddress()
                  + ": File already exists: "+ m_dst_fname;
      }
      else {
        ok = true;
      }
    }
    return ok;
  }
  void Send_Write_Response( final boolean ok )
  {
    try {
      Send_Write_Response_e( ok );
    }
    catch( IOException e )
    {
      Die("Send_Write_Response(): IOException: "+ e);
    }
  }
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
  void Send_Write_Response_e( final boolean ok ) throws IOException
  {
    Get_Out_Stream();

    if( m_running )
    {
      m_dout_stream.writeByte( OPCODE_PUT_RESP );

      if( ok ) {
        m_dout_stream.writeByte( 1 );
      }
      else {
        m_dout_stream.writeByte( 0 );
        m_dout_stream.writeInt( m_err_msg.length() );
        m_dout_stream.writeChars( m_err_msg );
        Die( m_err_msg );
      }
    }
  }

  void Receive_File_Data()
  {
    if( m_running )
    try {
      Receive_File_Data_e();
    }
    catch( FileNotFoundException e )
    {
      Die("Receive_File_Data(): FileNotFoundException: "+ e);
    }
    catch( IOException e )
    {
      Die("Receive_File_Data(): IOException: "+ e);
    }
  }
  //               |<1>|
  // Read/Write    -----------------
  // DATA          | 5 | File data |
  //               -----------------
  void Receive_File_Data_e() throws IOException, FileNotFoundException
  {
    final byte op_code = m_din_stream.readByte();

    if( op_code != OPCODE_DATA )
    {
      Die("Receive_File_Data(): Received bad OPCODE: " + op_code);
    }
    else {
      File dst_file = m_dst_path.toFile();
      FileOutputStream fos = new FileOutputStream( dst_file );
      BufferedOutputStream bos = new BufferedOutputStream( fos, 512 );

      final long st_time = System.currentTimeMillis();

      byte[] ba = new byte[512];

      long total_bytes_read = 0;

      while( total_bytes_read < m_dst_file_len )
      {
        final int bytes_read = m_din_stream.read( ba );

        if( 0 < bytes_read )
        {
          total_bytes_read += bytes_read;

          bos.write( ba, 0, bytes_read );
        }
      }
      bos.flush();
      bos.close();
      fos.close();
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

  boolean          m_running = true;
  String           m_dst_fname;
  Path             m_dst_path;
  int              m_dst_file_len;
  DataOutputStream m_dout_stream;
  String           m_err_msg;
}

