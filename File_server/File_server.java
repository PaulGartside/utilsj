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
import java.io.InputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

class File_server
{
  public static void main(String[] args)
  {
    if( args.length != 1 ) Usage();

    try {
      new File_server( args ).Run();
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

    m_running = false;
  }
  File_server( String[] args )
  {
    m_client_IP_str = args[0];
  }
  void Run()
  {
    Get_Client_Inet_Address();
    Create_Server_Socket();

    // Keep running until killed:
    while( m_running )
    {
      Request_Type req_type = Wait_4_Request();

      if( req_type == Request_Type.GET )
      {
        new Handle_Get( m_client_sock, m_din_stream ).Run();
      }
      else if( req_type == Request_Type.PUT )
      {
        new Handle_Put( m_client_sock, m_din_stream ).Run();
      }
      Clean_Up_Client();
    }
    Clean_Up_Server();
  }
  void Get_Client_Inet_Address()
  {
    try {
      m_client_inet_addr = InetAddress.getByName( m_client_IP_str );
    }
    catch( UnknownHostException e )
    {
      Die(m_client_IP_str + ": UnknownHostException: "+ e);
    }
  }
  void Create_Server_Socket()
  {
    if( m_running )
    try {
      m_server_sock = new ServerSocket( SERVER_PORT );
    }
    catch( IOException e )
    {
      Die("Create_Server_Socket(): IOException: "+ e);
    }
  }
  Request_Type Wait_4_Request()
  {
    Request_Type req_type = Request_Type.UNKNOWN;

    Accept_Client_Connection();

    if( m_running )
    if( ! m_client_sock.getInetAddress().equals( m_client_inet_addr ) )
    {
      Msg("Denied connection from: "+ m_client_sock.toString() );
    }
    else {
      Get_In_Stream();

      final byte request = Read_Request();

      if( request == OPCODE_GET_REQ )
      {
        req_type = Request_Type.GET;
      }
      else if( request == OPCODE_PUT_REQ )
      {
        req_type = Request_Type.PUT;
      }
    }
    return req_type;
  }
  void Accept_Client_Connection()
  {
    Msg("Listening on: "+ m_server_sock.getLocalSocketAddress() );

    try {
      m_client_sock = m_server_sock.accept();
    }
    catch( IOException e )
    {
      Die("Accept_Client_Connection(): IOException: " + e);
    }
  }
  byte Read_Request()
  {
    byte request = OPCODE_NONE;

    if( m_running )
    try {
      request = m_din_stream.readByte();
    }
    catch( IOException e )
    {
      Die("Read_Request(): IOException: " + e);
    }
    return request;
  }
  void Get_In_Stream()
  {
    if( m_running )
    try {
      InputStream in_stream = m_client_sock.getInputStream();

      m_din_stream = new DataInputStream( in_stream );
    }
    catch( IOException e )
    {
      Die("m_client_sock.getInputStream(): IOException: " + e);
    }
  }
  void Clean_Up_Client()
  {
    try {
      if( null != m_client_sock )
      {
        m_client_sock.close();
      }
      if( null != m_din_stream )
      {
        m_din_stream.close();
      }
    }
    catch( IOException e )
    {
      Msg("Clean_Up_Client(): IOException: " + e);
    }
  }
  void Clean_Up_Server()
  {
    try {
      if( null != m_server_sock )
      {
        m_server_sock.close();
      }
    }
    catch( IOException e )
    {
      Msg("Clean_Up_Server(): IOException: " + e);
    }
  }
  static final int  SERVER_PORT     = 6969;

  static final byte OPCODE_NONE     = 0;
  static final byte OPCODE_GET_REQ  = 1;
  static final byte OPCODE_GET_RESP = 2;
  static final byte OPCODE_PUT_REQ  = 3;
  static final byte OPCODE_PUT_RESP = 4;
  static final byte OPCODE_DATA     = 5;

  final String m_client_IP_str;

  boolean         m_running = true;
  InetAddress     m_client_inet_addr;
  ServerSocket    m_server_sock;
  Socket          m_client_sock;
  DataInputStream m_din_stream;
}

enum Request_Type
{
  UNKNOWN,
  GET,
  PUT
}

