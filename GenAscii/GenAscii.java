
import java.util.ArrayList;
import java.util.Random;

class GenAscii
{
  public static void main(String[] args)
  {
    if( args.length != 1 ) Usage();

    Init_Chars();
    Remove_Chars( '!', '/' );
    Remove_Chars( ':', '@' );
    Remove_Chars( '[', '`' );
    Remove_Chars( '{', '~' );

    long time_ms = System.currentTimeMillis();

    final int LENGTH = Integer.valueOf( args[0] );

    StringBuilder sb = new StringBuilder(LENGTH);
 
    Random rand = new Random();
    rand.setSeed( time_ms );

    double num_possiblities = 1;
    for( int k=0; k<LENGTH; k++ )
    {
      num_possiblities *= m_chars.size();
      final long number = rand.nextLong() ^ time_ms;
      final long remainder = Math.abs( number % m_chars.size() );
      final char C = remainder_2_char( (int)remainder );
      sb.append( C );

      // Sleep for a random amout of time:
      final int ms = (int)Math.abs( number % 101 );
      Sleep( ms );
      time_ms = System.currentTimeMillis();
    }
    System.out.println("Ascii="+sb.toString() );
    System.out.println("Possibilities="+num_possiblities );
  }
//public static void main(String[] args)
//{
//  Init_Chars();
//  Remove_Chars( 'A', 'Z' );
//  Show_Chars();
//}
  static void Usage()
  {
    System.out.println("usage: GenAscii number_of_characters");
    System.exit( 0 );
  }
  static char remainder_2_char( int remainder )
  {
    // Not using '`', which is 96
  //if( 96 <= remainder ) remainder++;

  //return (char)(remainder + (long)'!');
    return m_chars.get( remainder );
  }
  public static
  void Sleep( final int ms )
  {
    try { Thread.sleep( ms ); } catch( Exception e ) {}
  }
  static void Init_Chars()
  {
    for( int k=33; k<127; k++ )
    {
      m_chars.add( (char)k );
    }
  }
  static void Show_Chars()
  {
    for( int k=0; k<m_chars.size(); k++ )
    {
      System.out.println(k+ "("+ m_chars.get(k) +")");
    }
  }
  static void Remove_Chars( final char C1, final char C2 )
  {
    for( int k=0; k<m_chars.size(); k++ )
    {
      final char C = m_chars.get(k);

      if( C1 <= C && C <= C2 )
      {
        m_chars.remove( k );
        k--;
      }
    }
  }
  static ArrayList<Character> m_chars = new ArrayList<>();
}

