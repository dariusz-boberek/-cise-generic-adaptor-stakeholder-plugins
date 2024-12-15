package eu.cise.adaptor.plugin.xpath;

import eu.cise.servicemodel.v1.message.ConditionOperatorType;
import eu.cise.servicemodel.v1.message.PullRequest;
import eu.cise.servicemodel.v1.message.SelectorCondition;
import eu.eucise.xml.DefaultXmlMapper;
import eu.eucise.xml.XmlMapper;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class CisePayloadSelectorToXPath {

    private static final XPath xPath = XPathFactory.newInstance().newXPath();
    private static final XmlMapper xmlMapper = new DefaultXmlMapper();

    private CisePayloadSelectorToXPath() {
    }

    public static String getCiseConditionValue(SelectorCondition selectorCondition, PullRequest pullRequest) throws XPathExpressionException {
        String selector = selectorCondition.getSelector();
        XPathExpression selectorXPathExpression = xPath.compile(selector);
        return selectorXPathExpression.evaluate(xmlMapper.toDOM(pullRequest.getPayload()));
    }

    public static ConditionOperatorType getCiseConditionOperatorType(SelectorCondition selectorCondition) {
        return selectorCondition.getOperator();
    }

    public static String getSampleXPathForCiseMessage(SelectorCondition selectorCondition, ConditionOperatorType conditionOperatorType, String conditionValue) {
        String xPathQuery;
        String[] splitSelector = selectorCondition.getSelector().split("/");
        String xpathOperator = getXpathOperatorFromConditionOperatorType(conditionOperatorType);
        if (conditionOperatorType == ConditionOperatorType.IS_NULL) {
            xPathQuery = "//" + splitSelector[splitSelector.length - 1] + "[text()" + xpathOperator + "]/..";
        } else if (conditionOperatorType == ConditionOperatorType.LIKE || conditionOperatorType == ConditionOperatorType.NOT_LIKE) {
            xPathQuery = "//" + splitSelector[splitSelector.length - 1] + "[" + xpathOperator + "(text()," + conditionValue + ")]/..";
        } else {
            xPathQuery = "//" + splitSelector[splitSelector.length - 1] + "[text()" + xpathOperator + "'" + conditionValue + "']/..";
        }
        return xPathQuery;
    }

    private static String getXpathOperatorFromConditionOperatorType(ConditionOperatorType conditionOperatorType) {

        switch (conditionOperatorType) {
            case IS_NULL:
                return "=''";
            case NOT_LIKE:
                return "contains";
            case GREATER_THAN_OR_EQUAL_TO:
                return ">=";
            case NOT_EQUAL:
                return "!=";
            case LIKE:
                return "!contains";
            case EQUAL:
                return "=";
            case GREATER_THAN:
                return ">";
            case LESS_THAN_OR_EQUAL_TO:
                return "<=";
            case LESS_THAN:
                return "<";
            default:
                return null;
        }
    }
}