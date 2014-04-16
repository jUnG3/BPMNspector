package de.uniba.dsg.ppn.ba;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.oclc.purl.dsdl.svrl.FailedAssert;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.phloc.schematron.ISchematronResource;
import com.phloc.schematron.pure.SchematronResourcePure;

import de.uniba.dsg.bpmn.ValidationResult;
import de.uniba.dsg.bpmn.Violation;
import de.uniba.dsg.ppn.ba.helper.BpmnNamespaceContext;
import de.uniba.dsg.ppn.ba.helper.BpmnValidationException;
import de.uniba.dsg.ppn.ba.helper.PreProcessResult;

public class SchematronBPMNValidator {

	private DocumentBuilderFactory documentBuilderFactory;
	private DocumentBuilder documentBuilder;
	private XPathFactory xPathFactory;
	private XPath xpath;
	private XPathExpression xPathExpression;
	private PreProcessor preProcessor;
	private XmlLocator xmlLocator;
	public final static String bpmnNamespace = "http://www.omg.org/spec/BPMN/20100524/MODEL";
	final static String bpmndiNamespace = "http://www.omg.org/spec/BPMN/20100524/DI";

	{
		documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// ignore
		}
		xPathFactory = XPathFactory.newInstance();
		xpath = xPathFactory.newXPath();
		xpath.setNamespaceContext(new BpmnNamespaceContext());
		try {
			xPathExpression = xpath.compile("//bpmn:*/@id");
		} catch (XPathExpressionException e) {
			// ignore
		}
		preProcessor = new PreProcessor();
		xmlLocator = new XmlLocator();
	}

	// TODO: refactor
	public ValidationResult validate(File xmlFile)
			throws BpmnValidationException {
		final ISchematronResource schematronSchema = SchematronResourcePure
				.fromFile(SchematronBPMNValidator.class.getResource(
						"schematron/validation.xml").getPath());
		if (!schematronSchema.isValidSchematron()) {
			throw new BpmnValidationException("Invalid Schematron!");
		}

		ValidationResult validationResult = new ValidationResult();

		try {
			Document headFileDocument = documentBuilder.parse(xmlFile);
			File parentFolder = xmlFile.getParentFile();
			validationResult.getCheckedFiles().add(xmlFile.getAbsolutePath());

			checkConstraint001(xmlFile, parentFolder, validationResult);
			checkConstraint002(xmlFile, parentFolder, validationResult);

			List<String[]> namespaceTable = new ArrayList<>();
			PreProcessResult preProcessResult = preProcessor.preProcess(
					headFileDocument, parentFolder, namespaceTable);

			SchematronOutputType schematronOutputType = schematronSchema
					.applySchematronValidationToSVRL(new StreamSource(
							transformDocumentToInputStream(headFileDocument)));
			for (int i = 0; i < schematronOutputType
					.getActivePatternAndFiredRuleAndFailedAssertCount(); i++) {
				if (schematronOutputType
						.getActivePatternAndFiredRuleAndFailedAssertAtIndex(i) instanceof FailedAssert) {
					FailedAssert failedAssert = (FailedAssert) schematronOutputType
							.getActivePatternAndFiredRuleAndFailedAssertAtIndex(i);
					String message = failedAssert.getText().trim();
					String constraint = message.substring(0,
							message.indexOf('|'));
					String errorMessage = message.substring(message
							.indexOf('|') + 1);
					int line = xmlLocator.findLine(xmlFile,
							failedAssert.getLocation());
					String fileName = xmlFile.getName();
					String location = failedAssert.getLocation();
					if (line == -1) {
						try {
							String xpathId = "";
							if (failedAssert.getDiagnosticReferenceCount() > 0) {
								xpathId = failedAssert.getDiagnosticReference()
										.get(0).getText().trim();
							}
							String[] result = searchForViolationFile(xpathId,
									validationResult,
									preProcessResult.getNamespaceTable());
							fileName = result[0];
							line = Integer.valueOf(result[1]);
							location = result[2];
						} catch (BpmnValidationException e) {
							fileName = e.getMessage();
						}
					}
					validationResult.getViolations().add(
							new Violation(constraint, fileName, line, location,
									errorMessage));
				}
			}

			for (int i = 0; i < validationResult.getCheckedFiles().size(); i++) {
				File f = new File(validationResult.getCheckedFiles().get(i));
				validationResult.getCheckedFiles().set(i, f.getName());
			}
		} catch (SAXException | IOException e) {
			throw new BpmnValidationException(
					"Given file couldn't be read or doesn't exist!");
		} catch (Exception e) {
			throw new BpmnValidationException(
					"Something went wrong during schematron validation!");
		}

		validationResult.setValid(validationResult.getViolations().isEmpty());

		return validationResult;
	}

	public ByteArrayInputStream transformDocumentToInputStream(
			Document headFileDocument) throws UnsupportedEncodingException,
			TransformerException {

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer
				.transform(new DOMSource(headFileDocument), new StreamResult(
						new OutputStreamWriter(outputStream, "UTF-8")));

		ByteArrayInputStream inputStream = new ByteArrayInputStream(
				outputStream.toByteArray());

		return inputStream;
	}

	// TODO: refactor
	private String[] searchForViolationFile(String xpathExpression,
			ValidationResult validationResult, List<String[]> namespaceTable)
			throws BpmnValidationException {
		boolean search = true;
		String fileName = "";
		String line = "-1";
		String xpathObjectId = "";
		int i = 0;
		while (search && i < validationResult.getCheckedFiles().size()) {
			File checkedFile = new File(validationResult.getCheckedFiles().get(
					i));
			try {
				Document document = documentBuilder.parse(checkedFile);
				String namespacePrefix = xpathExpression.substring(0,
						xpathExpression.indexOf('_'));
				String namespace = "";
				for (String[] s : namespaceTable) {
					if (s[0].equals(namespacePrefix)) {
						namespace = s[1];
					}
				}
				for (String checkedFilePath : validationResult
						.getCheckedFiles()) {
					checkedFile = new File(checkedFilePath);
					try {
						document = documentBuilder.parse(checkedFile);
						if (document.getDocumentElement()
								.getAttribute("targetNamespace")
								.equals(namespace)) {
							xpathObjectId = createIdBpmnExpression(xpathExpression
									.substring(xpathExpression.indexOf('_') + 1));
							line = ""
									+ xmlLocator.findLine(checkedFile,
											xpathObjectId);
							xpathObjectId += "[0]";
							fileName = checkedFile.getName();
							search = false;
							break;
						}
					} catch (SAXException | IOException e) {
						// TODO Auto-generated catch block
					}
				}
			} catch (SAXException | IOException e) {
				// TODO Auto-generated catch block
			}
			i++;
		}

		if (search) {
			throw new BpmnValidationException("BPMN Element couldn't be found!");
		}

		return new String[] { fileName, line, xpathObjectId };
	}

	private void checkConstraint001(File headFile, File folder,
			ValidationResult validationResult) {
		try {
			Document headFileDocument = documentBuilder.parse(headFile);

			Object[][] importedFiles = preProcessor.selectImportedFiles(
					headFileDocument, folder, 0);

			for (int i = 0; i < importedFiles.length; i++) {
				if (!((File) importedFiles[i][0]).exists()) {
					String xpathLocation = "//bpmn:import[@location = '"
							+ ((File) importedFiles[i][0]).getName() + "']";
					validationResult.getViolations().add(
							new Violation("EXT.001",
									((File) importedFiles[i][0]).getName(),
									xmlLocator
											.findLine(headFile, xpathLocation),
									xpathLocation + "[0]",
									"The imported file does not exist"));
				} else {
					checkConstraint001(((File) importedFiles[i][0]), folder,
							validationResult);
				}
			}
		} catch (SAXException | IOException e) {
			// TODO Auto-generated catch block
		}
	}

	private void checkConstraint002(File headFile, File folder,
			ValidationResult validationResult) throws XPathExpressionException {
		List<File> importedFileList = searchForImports(headFile, folder,
				validationResult);

		for (int i = 0; i < importedFileList.size(); i++) {
			File file1 = importedFileList.get(i);
			Document document1;
			try {
				document1 = documentBuilder.parse(file1);
				preProcessor.removeBPMNDINode(document1);
				String namespace1 = document1.getDocumentElement()
						.getAttribute("targetNamespace");
				for (int j = i + 1; j < importedFileList.size(); j++) {
					File file2 = importedFileList.get(j);
					try {
						Document document2 = documentBuilder.parse(file2);
						preProcessor.removeBPMNDINode(document2);
						String namespace2 = document2.getDocumentElement()
								.getAttribute("targetNamespace");
						if (namespace1.equals(namespace2)) {
							checkNamespacesAndIdDuplicates(file1, file2,
									document1, document2, validationResult);
						}
					} catch (IOException | SAXException e) {
						// TODO Auto-generated catch block
					}
				}
			} catch (IOException | SAXException e) {
				// TODO Auto-generated catch block
			}
		}
	}

	// TODO: refactor
	private List<File> searchForImports(File file, File folder,
			ValidationResult validationResult) {
		List<File> importedFileList = new ArrayList<>();
		try {
			Document document = documentBuilder.parse(file);
			Object[][] importedFiles = preProcessor.selectImportedFiles(
					document, folder, 0);
			importedFileList.add(file);

			for (int i = 0; i < importedFiles.length; i++) {
				if (((File) importedFiles[i][0]).exists()) {
					validationResult.getCheckedFiles().add(
							((File) importedFiles[i][0]).getAbsolutePath());
					importedFileList.addAll(searchForImports(
							((File) importedFiles[i][0]), folder,
							validationResult));
				}
			}
		} catch (IOException | SAXException e) {
			// TODO Auto-generated catch block
		}

		return importedFileList;
	}

	private void checkNamespacesAndIdDuplicates(File file1, File file2,
			Document document1, Document document2,
			ValidationResult validationResult) throws XPathExpressionException {
		NodeList foundNodes1 = (NodeList) xPathExpression.evaluate(document1,
				XPathConstants.NODESET);
		NodeList foundNodes2 = (NodeList) xPathExpression.evaluate(document2,
				XPathConstants.NODESET);
		for (int k = 1; k < foundNodes1.getLength(); k++) {
			String importedFile1Id = foundNodes1.item(k).getNodeValue();
			for (int l = 1; l < foundNodes2.getLength(); l++) {
				String importedFile2Id = foundNodes2.item(l).getNodeValue();
				if (importedFile1Id.equals(importedFile2Id)) {
					String xpathLocation = createIdBpmnExpression(importedFile1Id);
					validationResult.getViolations().add(
							new Violation("EXT.002", file1.getName(),
									xmlLocator.findLine(file1, xpathLocation),
									xpathLocation + "[0]",
									"Files have id duplicates"));
					validationResult.getViolations().add(
							new Violation("EXT.002", file2.getName(),
									xmlLocator.findLine(file2, xpathLocation),
									xpathLocation + "[0]",
									"Files have id duplicates"));
				}
			}
		}
	}

	private String createIdBpmnExpression(String id) {
		return "//bpmn:*[@id = '" + id + "']";
	}

}
