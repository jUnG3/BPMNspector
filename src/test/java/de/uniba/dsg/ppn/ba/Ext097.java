package de.uniba.dsg.ppn.ba;

import org.junit.Test;

import de.uniba.dsg.bpmnspector.common.ValidationResult;
import de.uniba.dsg.bpmnspector.common.Violation;
import de.uniba.dsg.ppn.ba.helper.BpmnValidationException;

/**
 * Test class for testing Constraint EXT.097
 *
 * @author Philipp Neugebauer
 * @version 1.0
 *
 */
public class Ext097 extends TestCase {

    @Test
    public void testConstraintFail() throws BpmnValidationException {
        ValidationResult result = verifyInValidResult(
                createFile("fail_end_without_sub-events.bpmn"), 1);
        assertViolation(result.getViolations().get(0), 8);
    }

    @Test
    public void testConstraintSubFail() throws BpmnValidationException {
        ValidationResult result = verifyInValidResult(
                createFile("fail_with_sub-endevent.bpmn"), 1);
        assertViolation(result.getViolations().get(0), 10);
    }

    @Test
    public void testConstraintSubSuccess() throws BpmnValidationException {
        verifyValidResult(createFile("success_with_sub-events.bpmn"));
    }

    @Test
    public void testConstraintSuccess() throws BpmnValidationException {
        verifyValidResult(createFile("success_without_sub-events.bpmn"));

    }

    private void assertViolation(Violation v, int line) {
        assertViolation(v, "//bpmn:endEvent[0]", line);
    }

    @Override
    protected String getErrorMessage() {
        return "A Start event must be present when an End event is used in the same process level";
    }

    @Override
    protected String getExtNumber() {
        return "097";
    }
}
