package org.gbif.nameparser;

import com.google.gson.JsonSyntaxException;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;

import static org.junit.Assert.assertEquals;

public class ParserConfigsTest {

  @Test
  @Ignore("Requires CLB live API")
  public void loadFromAPI() throws InterruptedException, IOException {
    var cfg = new ParserConfigs();
    cfg.loadFromCLB();
  }

  @Test(expected = JsonSyntaxException.class)
  public void loadHtml() throws InterruptedException, IOException {
    var cfg = new ParserConfigs();
    cfg.load(URI.create("https://www.google.com"));
  }

  @Test
  public void load404() throws InterruptedException, IOException {
    var cfg = new ParserConfigs();
    int loaded = cfg.load(URI.create("https://api.checklistbank.org/doesNotExist/999"));
    assertEquals(0, loaded);
  }

  @Test
  public void norm() throws InterruptedException {
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
    assertNorm(" ( zúr strassen 1973 ) märkus", "( zúr Straßen, 1973 )Märkus");
    assertNorm(" ( zur strassen 1973 ) markus robert", "( zur Strassen, 1973 ) Markus & RObert");
    assertNorm("coccinella 2 pustulata linnaeus 1758", "Coccinella 2-puſtulata Linnæus, 1758");
  }

  private void assertNorm(String expect, String x) throws InterruptedException {
    //System.out.println(String.format("%50s -> %s", x, ParserConfigs.norm(x)));
    assertEquals(expect, ParserConfigs.norm(x));
  }
}