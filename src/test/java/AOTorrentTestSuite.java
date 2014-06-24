import org.aotorrent.bencode.ParserTest;
import org.aotorrent.bencode.ValueTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author dmitry 6/25/14 12:28 AM
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        ParserTest.class,
        ValueTest.class
})
public class AOTorrentTestSuite {
}
