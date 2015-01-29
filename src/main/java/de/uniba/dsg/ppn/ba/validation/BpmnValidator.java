package de.uniba.dsg.ppn.ba.validation;

import ch.qos.logback.classic.Level;
import api.ValidationResult;
import api.ValidationException;

import java.io.File;
import java.util.List;

/**
 * Interface for the implementation of the validator. Allows the usage of the
 * validator in other projects. The loglevel is set default to info. If you need
 * another log level, change the log level before the validation process.
 *
 * @author Philipp Neugebauer
 * @author Matthias Geiger
 * @version 1.0
 *
 */
public interface BpmnValidator {

    /**
     * returns the set loglevel of all loggers
     *
     * @return the set log level {@link ch.qos.logback.classic.Level}
     */
    Level getLogLevel();

    /**
     * Sets the loglevel of all loggers of the bpmn validator to the given level
     *
     * @param logLevel
     *            possible levels: {@link ch.qos.logback.classic.Level}
     */
    void setLogLevel(Level logLevel);

    /**
     * checks the given xmlFile for bpmn constraint violations
     *
     * @param xmlFile
     *            the xml file to validate
     * @return ValidationResult including all checked files and found violations
     * @throws ValidationException
     *             if something fails during validation process
     */
    ValidationResult validate(File xmlFile) throws ValidationException;

    /**
     * checks the given xmlFiles for bpmn constraint violations
     *
     * @param xmlFiles
     *            the list of xml files to validate
     * @return list of {@link de.uniba.dsg.bpmnspector.common.ValidationResult} including all
     *         checked files and found violations for each file
     * @throws ValidationException
     *             if something fails during validation process
     */
    List<ValidationResult> validateFiles(List<File> xmlFiles)
            throws ValidationException;

}
