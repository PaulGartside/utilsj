////////////////////////////////////////////////////////////////////////////////
// Generate Ascii Java Implementation                                         //
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

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GenAscii extends Application
{
  public static void main(String[] args)
  {
    Application.launch(args);
  }

  @Override
  public void start(Stage stage)
  {
    Init_Rand();
    Init_Chars();

    // Create a GridPane and set its background color to lightgray
    GridPane root = new GridPane();
    root.setHgap( 5 );
    root.setVgap( 5 );
    root.setStyle("-fx-background-color: lightgray;" +
                  "-fx-padding: 10;" + 
                  "-fx-border-style: solid inside;" + 
                  "-fx-border-width: 2;" +
                  "-fx-border-insets: 5;" + 
                  "-fx-border-radius: 5;" + 
                  "-fx-border-color: blue;");

    // Add children to the GridPane
    AddRow_0( root );
    AddRow_1( root );
    AddRow_2( root );
    AddRow_3( root );
    AddRow_4( root );
    AddRow_5( root );
    AddRow_6( root );
    AddRow_7( root );
    AddRow_8( root );
    AddRow_9( root );
    AddRow_A( root );

    Scene scene = new Scene(root);
    stage.setScene(scene);
    stage.setTitle("Generate Ascii");
    stage.show();
  }

  void Log( String msg )
  {
    System.out.println("GenAscii: "+ msg );
  }
  void Init_Rand()
  {
    long time_ms = System.currentTimeMillis();

    m_rand.setSeed( time_ms );
  }
  void Init_Chars()
  {
    for( int k=33; k<127; k++ )
    {
      m_chars.add( (char)k );
    }
  }
  void Add_Char( final char C )
  {
    m_chars.add( C );
  }
  boolean Remove_Char( final char C1 )
  {
    boolean removed = false;

    for( int k=0; !removed && k<m_chars.size(); k++ )
    {
      final char C = m_chars.get(k);

      if( C1 == C )
      {
        m_chars.remove( k );

        removed = true;
      }
    }
    return removed;
  }
  void AddRow_0( GridPane root )
  {
    final int col_span = 1;
    final int row_span = 1;
    final int row = 0;

    Button b_0 = mk_Button("~", root,  0, row, col_span, row_span);
    Button b_1 = mk_Button("!", root,  1, row, col_span, row_span);
    Button b_2 = mk_Button("@", root,  2, row, col_span, row_span);
    Button b_3 = mk_Button("#", root,  3, row, col_span, row_span);
    Button b_4 = mk_Button("$", root,  4, row, col_span, row_span);
    Button b_5 = mk_Button("%", root,  5, row, col_span, row_span);
    Button b_6 = mk_Button("^", root,  6, row, col_span, row_span);
    Button b_7 = mk_Button("&", root,  7, row, col_span, row_span);
    Button b_8 = mk_Button("*", root,  8, row, col_span, row_span);
    Button b_9 = mk_Button("(", root,  9, row, col_span, row_span);
    Button b10 = mk_Button(")", root, 10, row, col_span, row_span);
    Button b11 = mk_Button("_", root, 11, row, col_span, row_span);
    Button b12 = mk_Button("+", root, 12, row, col_span, row_span);

    m_special_buttons.add( b_0 );
    m_special_buttons.add( b_1 );
    m_special_buttons.add( b_2 );
    m_special_buttons.add( b_3 );
    m_special_buttons.add( b_4 );
    m_special_buttons.add( b_5 );
    m_special_buttons.add( b_6 );
    m_special_buttons.add( b_7 );
    m_special_buttons.add( b_8 );
    m_special_buttons.add( b_9 );
    m_special_buttons.add( b10 );
    m_special_buttons.add( b11 );
    m_special_buttons.add( b12 );
  }
  void AddRow_1( GridPane root )
  {
    final int col_span = 1;
    final int row_span = 1;
    final int row = 1;

    Button b_0 = mk_Button("`", root,  0, row, col_span, row_span);
    Button b_1 = mk_Button("1", root,  1, row, col_span, row_span);
    Button b_2 = mk_Button("2", root,  2, row, col_span, row_span);
    Button b_3 = mk_Button("3", root,  3, row, col_span, row_span);
    Button b_4 = mk_Button("4", root,  4, row, col_span, row_span);
    Button b_5 = mk_Button("5", root,  5, row, col_span, row_span);
    Button b_6 = mk_Button("6", root,  6, row, col_span, row_span);
    Button b_7 = mk_Button("7", root,  7, row, col_span, row_span);
    Button b_8 = mk_Button("8", root,  8, row, col_span, row_span);
    Button b_9 = mk_Button("9", root,  9, row, col_span, row_span);
    Button b10 = mk_Button("0", root, 10, row, col_span, row_span);
    Button b11 = mk_Button("-", root, 11, row, col_span, row_span);
    Button b12 = mk_Button("=", root, 12, row, col_span, row_span);

    m_number_buttons.add( b_1 );
    m_number_buttons.add( b_2 );
    m_number_buttons.add( b_3 );
    m_number_buttons.add( b_4 );
    m_number_buttons.add( b_5 );
    m_number_buttons.add( b_6 );
    m_number_buttons.add( b_7 );
    m_number_buttons.add( b_8 );
    m_number_buttons.add( b_9 );
    m_number_buttons.add( b10 );

    m_special_buttons.add( b_0 );
    m_special_buttons.add( b11 );
    m_special_buttons.add( b12 );
  }
  void AddRow_2( GridPane root )
  {
    final int col_span = 1;
    final int row_span = 1;
    final int row = 2;

    Button b_0 = mk_Button("Q", root,  0, row, col_span, row_span);
    Button b_1 = mk_Button("W", root,  1, row, col_span, row_span);
    Button b_2 = mk_Button("E", root,  2, row, col_span, row_span);
    Button b_3 = mk_Button("R", root,  3, row, col_span, row_span);
    Button b_4 = mk_Button("T", root,  4, row, col_span, row_span);
    Button b_5 = mk_Button("Y", root,  5, row, col_span, row_span);
    Button b_6 = mk_Button("U", root,  6, row, col_span, row_span);
    Button b_7 = mk_Button("I", root,  7, row, col_span, row_span);
    Button b_8 = mk_Button("O", root,  8, row, col_span, row_span);
    Button b_9 = mk_Button("P", root,  9, row, col_span, row_span);
    Button b10 = mk_Button("{", root, 10, row, col_span, row_span);
    Button b11 = mk_Button("}", root, 11, row, col_span, row_span);
    Button b12 = mk_Button("|", root, 12, row, col_span, row_span);

    m_UC_let_buttons.add( b_0 );
    m_UC_let_buttons.add( b_1 );
    m_UC_let_buttons.add( b_2 );
    m_UC_let_buttons.add( b_3 );
    m_UC_let_buttons.add( b_4 );
    m_UC_let_buttons.add( b_5 );
    m_UC_let_buttons.add( b_6 );
    m_UC_let_buttons.add( b_7 );
    m_UC_let_buttons.add( b_8 );
    m_UC_let_buttons.add( b_9 );

    m_special_buttons.add( b10 );
    m_special_buttons.add( b11 );
    m_special_buttons.add( b12 );
  }
  void AddRow_3( GridPane root )
  {
    final int col_span = 1;
    final int row_span = 1;
    final int row = 3;

    Button b_0 = mk_Button( "q", root,  0, row, col_span, row_span );
    Button b_1 = mk_Button( "w", root,  1, row, col_span, row_span );
    Button b_2 = mk_Button( "e", root,  2, row, col_span, row_span );
    Button b_3 = mk_Button( "r", root,  3, row, col_span, row_span );
    Button b_4 = mk_Button( "t", root,  4, row, col_span, row_span );
    Button b_5 = mk_Button( "y", root,  5, row, col_span, row_span );
    Button b_6 = mk_Button( "u", root,  6, row, col_span, row_span );
    Button b_7 = mk_Button( "i", root,  7, row, col_span, row_span );
    Button b_8 = mk_Button( "o", root,  8, row, col_span, row_span );
    Button b_9 = mk_Button( "p", root,  9, row, col_span, row_span );
    Button b10 = mk_Button( "[", root, 10, row, col_span, row_span );
    Button b11 = mk_Button( "]", root, 11, row, col_span, row_span );
    Button b12 = mk_Button("\\", root, 12, row, col_span, row_span );

    m_LC_let_buttons.add( b_0 );
    m_LC_let_buttons.add( b_1 );
    m_LC_let_buttons.add( b_2 );
    m_LC_let_buttons.add( b_3 );
    m_LC_let_buttons.add( b_4 );
    m_LC_let_buttons.add( b_5 );
    m_LC_let_buttons.add( b_6 );
    m_LC_let_buttons.add( b_7 );
    m_LC_let_buttons.add( b_8 );
    m_LC_let_buttons.add( b_9 );

    m_special_buttons.add( b10 );
    m_special_buttons.add( b11 );
    m_special_buttons.add( b12 );
  }
  void AddRow_4( GridPane root )
  {
    final int col_span = 1;
    final int row_span = 1;
    final int row = 4;

    Button b_0 = mk_Button(  "A", root,  0, row, col_span, row_span );
    Button b_1 = mk_Button(  "S", root,  1, row, col_span, row_span );
    Button b_2 = mk_Button(  "D", root,  2, row, col_span, row_span );
    Button b_3 = mk_Button(  "F", root,  3, row, col_span, row_span );
    Button b_4 = mk_Button(  "G", root,  4, row, col_span, row_span );
    Button b_5 = mk_Button(  "H", root,  5, row, col_span, row_span );
    Button b_6 = mk_Button(  "J", root,  6, row, col_span, row_span );
    Button b_7 = mk_Button(  "K", root,  7, row, col_span, row_span );
    Button b_8 = mk_Button(  "L", root,  8, row, col_span, row_span );
    Button b_9 = mk_Button(  ":", root,  9, row, col_span, row_span );
    Button b10 = mk_Button( "\"", root, 10, row, col_span, row_span );
         m_0_9 = new Button("0-9");

    m_0_9.setStyle( m_btn_style_blue );

    m_0_9.setMaxWidth(Double.MAX_VALUE);
    m_0_9.setMaxHeight(Double.MAX_VALUE);

    root.add( m_0_9, 11, row, 2, row_span );
    root.setHgrow( m_0_9, Priority.ALWAYS);
    root.setVgrow( m_0_9, Priority.ALWAYS);

    m_0_9.setOnAction( e -> _09Handler( e ) );

    m_UC_let_buttons.add( b_0 );
    m_UC_let_buttons.add( b_1 );
    m_UC_let_buttons.add( b_2 );
    m_UC_let_buttons.add( b_3 );
    m_UC_let_buttons.add( b_4 );
    m_UC_let_buttons.add( b_5 );
    m_UC_let_buttons.add( b_6 );
    m_UC_let_buttons.add( b_7 );
    m_UC_let_buttons.add( b_8 );

    m_special_buttons.add( b_9 );
    m_special_buttons.add( b10 );
  }
  void AddRow_5( GridPane root )
  {
    final int col_span = 1;
    final int row_span = 1;
    final int row = 5;

    Button b_0 = mk_Button(  "a", root,  0, row, col_span, row_span );
    Button b_1 = mk_Button(  "s", root,  1, row, col_span, row_span );
    Button b_2 = mk_Button(  "d", root,  2, row, col_span, row_span );
    Button b_3 = mk_Button(  "f", root,  3, row, col_span, row_span );
    Button b_4 = mk_Button(  "g", root,  4, row, col_span, row_span );
    Button b_5 = mk_Button(  "h", root,  5, row, col_span, row_span );
    Button b_6 = mk_Button(  "j", root,  6, row, col_span, row_span );
    Button b_7 = mk_Button(  "k", root,  7, row, col_span, row_span );
    Button b_8 = mk_Button(  "l", root,  8, row, col_span, row_span );
    Button b_9 = mk_Button(  ";", root,  9, row, col_span, row_span );
    Button b10 = mk_Button(  "'", root, 10, row, col_span, row_span );
         m_A_Z = new Button("A-Z");

    m_A_Z.setStyle( m_btn_style_blue );

    m_A_Z.setMaxWidth(Double.MAX_VALUE);
    m_A_Z.setMaxHeight(Double.MAX_VALUE);

    root.add( m_A_Z, 11, row, 2, row_span );
    root.setHgrow( m_A_Z, Priority.ALWAYS);
    root.setVgrow( m_A_Z, Priority.ALWAYS);

    m_A_Z.setOnAction( e -> A_ZHandler( e ) );

    m_LC_let_buttons.add( b_0 );
    m_LC_let_buttons.add( b_1 );
    m_LC_let_buttons.add( b_2 );
    m_LC_let_buttons.add( b_3 );
    m_LC_let_buttons.add( b_4 );
    m_LC_let_buttons.add( b_5 );
    m_LC_let_buttons.add( b_6 );
    m_LC_let_buttons.add( b_7 );
    m_LC_let_buttons.add( b_8 );

    m_special_buttons.add( b_9 );
    m_special_buttons.add( b10 );
  }
  void AddRow_6( GridPane root )
  {
    final int col_span = 1;
    final int row_span = 1;
    final int row = 6;

    Button b_0 = mk_Button( "Z", root,  0, row, col_span, row_span );
    Button b_1 = mk_Button( "X", root,  1, row, col_span, row_span );
    Button b_2 = mk_Button( "C", root,  2, row, col_span, row_span );
    Button b_3 = mk_Button( "V", root,  3, row, col_span, row_span );
    Button b_4 = mk_Button( "B", root,  4, row, col_span, row_span );
    Button b_5 = mk_Button( "N", root,  5, row, col_span, row_span );
    Button b_6 = mk_Button( "M", root,  6, row, col_span, row_span );
    Button b_7 = mk_Button( "<", root,  7, row, col_span, row_span );
    Button b_8 = mk_Button( ">", root,  8, row, col_span, row_span );
    Button b_9 = mk_Button( "?", root,  9, row, col_span, row_span );
         m_a_z = new Button("a-z");

    m_a_z.setStyle( m_btn_style_blue );

    m_a_z.setMaxWidth(Double.MAX_VALUE);
    m_a_z.setMaxHeight(Double.MAX_VALUE);

    root.add( m_a_z, 11, row, 2, row_span );
    root.setHgrow( m_a_z, Priority.ALWAYS);
    root.setVgrow( m_a_z, Priority.ALWAYS);

    m_a_z.setOnAction( e -> a_zHandler( e ) );

    m_UC_let_buttons.add( b_0 );
    m_UC_let_buttons.add( b_1 );
    m_UC_let_buttons.add( b_2 );
    m_UC_let_buttons.add( b_3 );
    m_UC_let_buttons.add( b_4 );
    m_UC_let_buttons.add( b_5 );
    m_UC_let_buttons.add( b_6 );

    m_special_buttons.add( b_7 );
    m_special_buttons.add( b_8 );
    m_special_buttons.add( b_9 );
  }
  void AddRow_7( GridPane root )
  {
    final int col_span = 1;
    final int row_span = 1;
    final int row = 7;

    Button b_0 = mk_Button("z", root,  0, row, col_span, row_span );
    Button b_1 = mk_Button("x", root,  1, row, col_span, row_span );
    Button b_2 = mk_Button("c", root,  2, row, col_span, row_span );
    Button b_3 = mk_Button("v", root,  3, row, col_span, row_span );
    Button b_4 = mk_Button("b", root,  4, row, col_span, row_span );
    Button b_5 = mk_Button("n", root,  5, row, col_span, row_span );
    Button b_6 = mk_Button("m", root,  6, row, col_span, row_span );
    Button b_7 = mk_Button(",", root,  7, row, col_span, row_span );
    Button b_8 = mk_Button(".", root,  8, row, col_span, row_span );
    Button b_9 = mk_Button("/", root,  9, row, col_span, row_span );
     m_special = new Button("special");

    m_special.setStyle( m_btn_style_blue );

    m_special.setMaxWidth(Double.MAX_VALUE);
    m_special.setMaxHeight(Double.MAX_VALUE);

    root.add( m_special, 10, row, 3, row_span );
    root.setHgrow( m_special, Priority.ALWAYS);
    root.setVgrow( m_special, Priority.ALWAYS);

    m_special.setOnAction( e -> specialHandler( e ) );

    m_LC_let_buttons.add( b_0 );
    m_LC_let_buttons.add( b_1 );
    m_LC_let_buttons.add( b_2 );
    m_LC_let_buttons.add( b_3 );
    m_LC_let_buttons.add( b_4 );
    m_LC_let_buttons.add( b_5 );
    m_LC_let_buttons.add( b_6 );

    m_special_buttons.add( b_7 );
    m_special_buttons.add( b_8 );
    m_special_buttons.add( b_9 );
  }
  void AddRow_8( GridPane root )
  {
    final int row_span = 1;
    final int row = 8;
    Label  pass_label = new Label("Password:");
         m_pass_field = new TextField();
    Font f = Font.font( "Courier", FontWeight.NORMAL, m_pass_field.getHeight()-2 );
         m_pass_field.setFont( f );

    final int lbl_col      =  0;
    final int lbl_col_span =  2;
    final int fld_col      =  3;
    final int fld_col_span = 10;

      pass_label.setMaxWidth(Double.MAX_VALUE);   pass_label.setMaxHeight(Double.MAX_VALUE);
    m_pass_field.setMaxWidth(Double.MAX_VALUE); m_pass_field.setMaxHeight(Double.MAX_VALUE);

    root.add(  pass_label, lbl_col, row, lbl_col_span, row_span);
    root.add(m_pass_field, fld_col, row, fld_col_span, row_span);

    GridPane.setVgrow(  pass_label, Priority.ALWAYS);
    GridPane.setVgrow(m_pass_field, Priority.ALWAYS);
  }
  void AddRow_9( GridPane root )
  {
    final int row_span = 1;
    final int row = 9;
    Label name_label = new Label("Possibilities:");
        m_posi_label = new Label("0");

    final int name_col      =  0;
    final int name_col_span =  3;
    final int  pos_col      =  3;
    final int  pos_col_span = 10;

      name_label.setMaxWidth(Double.MAX_VALUE);   name_label.setMaxHeight(Double.MAX_VALUE);
    m_posi_label.setMaxWidth(Double.MAX_VALUE); m_posi_label.setMaxHeight(Double.MAX_VALUE);

    root.add(  name_label, name_col, row, name_col_span, row_span);
    root.add(m_posi_label,  pos_col, row,  pos_col_span, row_span);

    GridPane.setVgrow(  name_label, Priority.ALWAYS);
    GridPane.setVgrow(m_posi_label, Priority.ALWAYS);
  }
  void AddRow_A( GridPane root )
  {
    final int col_span = 2;
    final int row_span = 1;
    final int row = 10;

    Button b_gener = new Button("Generate");
    Button b_clear = new Button("Clear");
    Button b_close = new Button("Close");

    b_gener.setOnAction( e -> GenerateHandler( e ) );
    b_clear.setOnAction( e -> ClearHandler( e ) );
    b_close.setOnAction( e -> CloseHandler( e ) );

    b_gener.setMaxWidth(Double.MAX_VALUE); b_gener.setMaxHeight(Double.MAX_VALUE);
    b_clear.setMaxWidth(Double.MAX_VALUE); b_clear.setMaxHeight(Double.MAX_VALUE);
    b_close.setMaxWidth(Double.MAX_VALUE); b_close.setMaxHeight(Double.MAX_VALUE);

    root.add(b_gener,  2, row, 3       , row_span);
    root.add(b_clear,  6, row, col_span, row_span);
    root.add(b_close,  9, row, col_span, row_span);

    root.setHgrow( b_gener, Priority.ALWAYS); root.setVgrow( b_gener, Priority.ALWAYS);
    root.setHgrow( b_clear, Priority.ALWAYS); root.setVgrow( b_clear, Priority.ALWAYS);
    root.setHgrow( b_close, Priority.ALWAYS); root.setVgrow( b_close, Priority.ALWAYS);
  }
  Button mk_Button( String label )
  {
    Button b = new Button( label );

    b.setStyle( m_btn_style_blue );

    b.setMaxWidth(Double.MAX_VALUE);
    b.setMaxHeight(Double.MAX_VALUE);

    return b;
  }
  Button mk_Button( String label
                  , GridPane root
                  , int col
                  , int row
                  , int col_span
                  , int row_span )
  {
    Button b = new Button( label );

    b.setStyle( m_btn_style_blue );

    b.setMaxWidth(Double.MAX_VALUE);
    b.setMaxHeight(Double.MAX_VALUE);

    root.add( b, col, row, col_span, row_span );
    root.setHgrow( b, Priority.ALWAYS);
    root.setVgrow( b, Priority.ALWAYS);

    b.setOnAction( e -> ButtonHandler( e ) );

    m_map.put( b, label.charAt(0) );

    return b;
  }
//void ButtonHeight_CB()
//{
//}
  void ButtonHandler( ActionEvent e )
  {
    // Button -> char
    Object o = e.getSource();
    Button b = (Button)o;

    Character C = m_map.get( b );
    boolean removed = Remove_Char( C );

    if( removed ) b.setStyle(m_btn_style_red);
    else {
      Add_Char( C );
      b.setStyle(m_btn_style_blue);
    }
//System.out.println("C:"+C+" :removed: "+ removed);
  }
  void specialHandler( ActionEvent e )
  {
    if( special_on )
    {
      special_on = false;
      m_special.setStyle(m_btn_style_red);
    }
    else {
      special_on = true;
      m_special.setStyle(m_btn_style_blue);
    }

    for( int k=0; k<m_special_buttons.size(); k++ )
    {
      Button b = m_special_buttons.get( k );
      Character C = m_map.get( b );
      boolean removed = Remove_Char( C );

      if( removed ) b.setStyle(m_btn_style_red);
      else {
        Add_Char( C );
        b.setStyle(m_btn_style_blue);
      }
    }
  }
  void _09Handler( ActionEvent e )
  {
    if( _09_on )
    {
      _09_on = false;
      m_0_9.setStyle(m_btn_style_red);
    }
    else {
      _09_on = true;
      m_0_9.setStyle(m_btn_style_blue);
    }

    for( int k=0; k<m_number_buttons.size(); k++ )
    {
      Button b = m_number_buttons.get( k );
      Character C = m_map.get( b );
      boolean removed = Remove_Char( C );

      if( removed ) b.setStyle(m_btn_style_red);
      else {
        Add_Char( C );
        b.setStyle(m_btn_style_blue);
      }
    }
  }
  void A_ZHandler( ActionEvent e )
  {
    if( A_Z_on )
    {
      A_Z_on = false;
      m_A_Z.setStyle(m_btn_style_red);
    }
    else {
      A_Z_on = true;
      m_A_Z.setStyle(m_btn_style_blue);
    }

    for( int k=0; k<m_UC_let_buttons.size(); k++ )
    {
      Button b = m_UC_let_buttons.get( k );
      Character C = m_map.get( b );
      boolean removed = Remove_Char( C );

      if( removed ) b.setStyle(m_btn_style_red);
      else {
        Add_Char( C );
        b.setStyle(m_btn_style_blue);
      }
    }
  }
  void a_zHandler( ActionEvent e )
  {
    if( a_z_on )
    {
      a_z_on = false;
      m_a_z.setStyle(m_btn_style_red);
    }
    else {
      a_z_on = true;
      m_a_z.setStyle(m_btn_style_blue);
    }

    for( int k=0; k<m_LC_let_buttons.size(); k++ )
    {
      Button b = m_LC_let_buttons.get( k );
      Character C = m_map.get( b );
      boolean removed = Remove_Char( C );

      if( removed ) b.setStyle(m_btn_style_red);
      else {
        Add_Char( C );
        b.setStyle(m_btn_style_blue);
      }
    }
  }
  void GenerateHandler( ActionEvent e )
  {
    final long time_ms = System.currentTimeMillis();
    final long number = m_rand.nextLong() ^ time_ms;
    final long remainder = Math.abs( number % m_chars.size() );
    final char C = m_chars.get( (int)remainder );
    m_sb.append( C );

    if( Long.MAX_VALUE/m_chars.size() < m_num_possiblities )
    {
      m_num_possiblities = Long.MAX_VALUE;
    }
    else {
      m_num_possiblities *= m_chars.size();
    }
    m_posi_label.setText( Possibilities_2_Str() );
    m_pass_field.setText( m_sb.toString() );
  }
  void ClearHandler( ActionEvent e )
  {
    m_num_possiblities = 1L;
    m_sb.setLength( 0 );

    m_posi_label.setText( "0" );
    m_pass_field.setText( m_sb.toString() );
  }
  void CloseHandler( ActionEvent e )
  {
    System.exit( 0 );
  }
  String Possibilities_2_Str()
  {
    m_sb2.setLength( 0 );
    m_sb3.setLength( 0 );
    m_sb2.append( m_num_possiblities.toString() );

    int count = 0;
    for( int k=m_sb2.length()-1; 0<=k; k-- )
    {
      m_sb3.append( m_sb2.charAt( k ) );
      count++;
      if( count%3 == 0 && 0<k )
      {
        m_sb3.append( ',' );
      }
    }
    int thousands_chopped = 0;
    while( 8 < m_sb3.length() )
    {
      m_sb3.deleteCharAt(0); //< Remove three digits
      m_sb3.deleteCharAt(0); //< and comma
      m_sb3.deleteCharAt(0);
      m_sb3.deleteCharAt(0);
      thousands_chopped++;
    }
    m_sb3.reverse();
    m_sb3.append( Thousands_chopped_2_str(thousands_chopped) );
    return m_sb3.toString();
  }
  String Thousands_chopped_2_str( final int TC )
  {
    if     ( TC == 0 ) return "";
    else if( TC == 1 ) return " thousand";    // 10^3
    else if( TC == 2 ) return " million";     // 10^6
    else if( TC == 3 ) return " billion";     // 10^9
    else if( TC == 4 ) return " trillion";    // 10^12
    else if( TC == 5 ) return " quadrillion"; // 10^15
    else if( TC == 6 ) return " quintillion"; // 10^18
    else if( TC == 7 ) return " sextillion";  // 10^21
    else if( TC == 8 ) return " septillion";  // 10^24

    return Integer.toString( TC );
  }

  String m_btn_style_blue = "-fx-background-color: skyblue";
  String m_btn_style_red  = "-fx-background-color: pink";
  String m_btn_style_tan  = "-fx-background-color: wheat";

  TextField m_pass_field = new TextField();
  Label     m_posi_label = new Label("0");

  Map<Button,Character> m_map = new HashMap<>();
  List<Character> m_chars = new ArrayList<>();
  List<Button> m_number_buttons = new ArrayList<>();
  List<Button> m_LC_let_buttons = new ArrayList<>();
  List<Button> m_UC_let_buttons = new ArrayList<>();
  List<Button> m_special_buttons= new ArrayList<>();
  Button m_special;
  Button m_0_9;
  Button m_A_Z;
  Button m_a_z;
  boolean special_on = true;
  boolean _09_on = true;
  boolean A_Z_on = true;
  boolean a_z_on = true;
  StringBuilder m_sb = new StringBuilder();
  StringBuilder m_sb2 = new StringBuilder();
  StringBuilder m_sb3 = new StringBuilder();
  Long m_num_possiblities = 1L;
  Random m_rand = new Random();
}

