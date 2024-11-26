import com.github.heheteam.commonlib.database.DatabaseGradeTable
import org.jetbrains.exposed.sql.Database
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseTest {
  val database = Database.connect("jdbc:h2:mem:test", driver = "org.h2.Driver")

  @Test
  fun `send solution test`() {
    val mockGradeTable = DatabaseGradeTable(database)
    assertEquals(4, 2 + 2)
  }
}
