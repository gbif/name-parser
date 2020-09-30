package org.gbif.nameparser;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

public class ParserConfigsTest {

  @Test
  public void norm() {
    assertNorm("zur strassen 1973", "zur Strassen,  1973");
    assertNorm("zur strassen 1973", "zur Strassen , 1973");
    assertNorm("zur strassen 1973", " zur Strassen, 1973");
    assertNorm("zur strassen 1973", "zur Strassen,1973");
    assertNorm("zur strassen 1973", "zur  Strassen 1973 ");
    assertNorm(" ( zur strassen 1973 ) ", "(zur Strassen, 1973)");
    assertNorm(" ( zur ) strassen 1973", "(zur) Strassen 1973");
    assertNorm(" ( zur strassen 1973 ) markus", "(zur Strassen, 1973) Markus");
    assertNorm(" ( zur strassen 1973 ) markus", "( zur Strassen 1973 )  Markus  ");
    assertNorm(" ( zur strassen 1973 ) markus", " ( zur Strassen, 1973 )Markus");
    assertNorm(" ( zur strassen 1973 ) markus", "( zur Strassen, 1973 )Markus");
    assertNorm(" ( zúr straßen 1973 ) märkus", "( zúr Straßen, 1973 )Märkus");
    assertNorm(" ( zur strassen 1973 ) markus robert", "( zur Strassen, 1973 ) Markus & RObert");
  }

  private void assertNorm(String expect, String x) {
    //System.out.println(String.format("%50s -> %s", x, ParserConfigs.norm(x)));
    assertEquals(expect, ParserConfigs.norm(x));
  }
}