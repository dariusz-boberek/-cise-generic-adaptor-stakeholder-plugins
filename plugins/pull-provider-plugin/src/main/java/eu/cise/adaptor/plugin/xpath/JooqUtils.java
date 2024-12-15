package eu.cise.adaptor.plugin.xpath;

import eu.cise.servicemodel.v1.message.ConditionOperatorType;
import org.jooq.Condition;

public class JooqUtils {

    private JooqUtils() {
    }

    public static Condition createCondition(ConditionOperatorType operatorType, org.jooq.Field<Object> field, String value) {
        switch (operatorType) {
            case EQUAL:
                return field.eq(value);
            case NOT_EQUAL:
                return field.ne(value);
            default:
                throw new IllegalArgumentException("not implemented in showcase operator type: " + operatorType);
        }
    }
}
