package de.uniba.dsg.ppn.ba;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class Ext099 {

	// TODO: missing test case: fail_call_ref_process.bpmn

	@Test
	public void testConstraintEventFail() throws Exception {
		File f = new File(
				"D:\\Philipp\\BA\\testprocesses\\099\\fail_event.bpmn");
		boolean valid = SchematronBPMNValidator.validateViaPureSchematron(f);
		assertEquals(valid, false);
		assertEquals(
				SchematronBPMNValidator.getErrors(),
				"//bpmn:process[./@id = string(//bpmn:callActivity/@calledElement)][0]: Referenced process must have at least one None Start Event");
	}

	@Test
	public void testConstraintEventRefFail() throws Exception {
		File f = new File(
				"D:\\Philipp\\BA\\testprocesses\\099\\fail_eventref.bpmn");
		boolean valid = SchematronBPMNValidator.validateViaPureSchematron(f);
		assertEquals(valid, false);
		assertEquals(
				SchematronBPMNValidator.getErrors(),
				"//bpmn:process[./@id = string(//bpmn:callActivity/@calledElement)][0]: Referenced process must have at least one None Start Event");
	}

	@Test
	public void testConstraintSuccess() throws Exception {
		File f = new File("D:\\Philipp\\BA\\testprocesses\\099\\success.bpmn");
		boolean valid = SchematronBPMNValidator.validateViaPureSchematron(f);
		assertEquals(valid, true);
		assertEquals(SchematronBPMNValidator.getErrors(), "");
	}

	@Test
	public void testConstraintGlobalSuccess() throws Exception {
		File f = new File(
				"D:\\Philipp\\BA\\testprocesses\\099\\success_global.bpmn");
		boolean valid = SchematronBPMNValidator.validateViaPureSchematron(f);
		assertEquals(valid, true);
		assertEquals(SchematronBPMNValidator.getErrors(), "");
	}
}
