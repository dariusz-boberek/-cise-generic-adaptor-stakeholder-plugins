package eu.cise.adaptor.plugin;

import eu.cise.servicemodel.v1.message.ConditionOperatorType;
import org.h2.tools.RunScript;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;

import static eu.cise.adaptor.plugin.xpath.JooqUtils.createCondition;
import static org.assertj.core.api.Assertions.assertThat;


class SampleTranslationToSQLTest {

    private DSLContext context;

    @BeforeEach
    public void setup() throws Exception {
        //create and run in-memory DB
        String url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
        Connection connection = DriverManager.getConnection(url, "sa", "");
        context = DSL.using(connection, SQLDialect.H2);

        //populate in-memory DB
        RunScript.execute(connection, new StringReader(
                "CREATE TABLE vessel (vessel_name VARCHAR(255), imo_number VARCHAR(255)); " +
                        "INSERT INTO vessel (vessel_name, imo_number) VALUES ('Louis', '9074729');"
        ));
    }

    @AfterEach
    public void cleanup() throws Exception {
        context.execute("DROP TABLE vessel;");
    }

    @Test
    void it_creates_sql_string_in_isolation_from_database() throws Exception {
        //given
        String conditionValue = "9074729";
        String selectField = "vessel_name";
        String conditionField = "imo_number";
        String tableName = "vessel";
        ConditionOperatorType conditionOperatorType = ConditionOperatorType.EQUAL;
        Condition condition = createCondition(conditionOperatorType, DSL.field(conditionField), conditionValue);

        //when
        DSLContext tempContext = DSL.using(SQLDialect.H2);
        String sqlQuery = tempContext.select(DSL.field(selectField))
                .from(DSL.table(tableName))
                .where(condition)
                .getSQL(ParamType.INLINED);

        // 3. Assert SQL query is as expected
        String expectedSql = "select vessel_name from vessel where imo_number = '9074729'";
        assertThat(sqlQuery).isEqualTo(expectedSql);
    }

    @Test
    void it_executes_sql_string_against_in_memory_database_and_fetches_correct_response() {
        // Given
        String titanicImoNumber = "9074729";
        ConditionOperatorType conditionOperatorType = ConditionOperatorType.EQUAL;
        Condition condition = createCondition(conditionOperatorType, DSL.field("imo_number"), titanicImoNumber);

        // When
        String result = context.select(DSL.field("vessel_name"))
                .from("vessel")
                .where(condition)
                .fetchOne()
                .getValue(DSL.field("vessel_name", String.class));

        // Then
        assertThat(result).isEqualTo("Louis");
    }
}
