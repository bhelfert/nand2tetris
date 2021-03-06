package de.bhelfert.nand2tetris.vmtranslator;

import org.assertj.core.util.Files;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static org.junit.Assert.fail;

public class VMTranslatorAcceptanceTest {

    private static final String SRC_TEST_RESOURCES_PATH = "src/test/resources/";

    @Test
    public void simpleAdd() {
        translateAndTest("StackArithmetic/SimpleAdd/SimpleAdd");
    }

    @Test
    public void stackTest() {
        translateAndTest("StackArithmetic/StackTest/StackTest");
    }

    @Test
    public void basicTest() {
        translateAndTest("MemoryAccess/BasicTest/BasicTest");
    }

    @Test
    public void pointerTest() {
        translateAndTest("MemoryAccess/PointerTest/PointerTest");
    }

    @Test
    public void staticTest() {
        translateAndTest("MemoryAccess/StaticTest/StaticTest");
    }

    private void translateAndTest(String relativePathToFileWithoutExtension) {
        String fileNameWithoutExtension = SRC_TEST_RESOURCES_PATH + relativePathToFileWithoutExtension;
        translateVmFile(fileNameWithoutExtension);
        runTestScriptWithCpuEmulator(fileNameWithoutExtension);
    }

    private void translateVmFile(String fileNameWithoutExtension) {
        new VMTranslator(new String[] { fileNameWithoutExtension + ".vm" } ).translate();
    }

    private void runTestScriptWithCpuEmulator(String fileNameWithoutExtension) {
        String testFileName = fileNameWithoutExtension + ".tst";
        Process cpuEmulator = createCpuEmulatorProcess(testFileName);
        checkTestScriptResult(cpuEmulator, testFileName);
    }

    private Process createCpuEmulatorProcess(String testFileName) {
        try {
            return new ProcessBuilder("../../../tools/CPUEmulator.sh", testFileName)
                    .directory(Files.currentFolder())
                    .start();
        }
        catch (Exception e) {
            throw new RuntimeException("Could not run test [" + testFileName + "] using CPUEmulator.sh", e);
        }
    }

    private void checkTestScriptResult(Process cpuEmulator, String testFileName) {
        try (BufferedReader successReader = new BufferedReader(new InputStreamReader(cpuEmulator.getInputStream()))) {
            if (!"End of script - Comparison ended successfully".equals(successReader.readLine())) {
                failWithError(cpuEmulator, testFileName);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Could not read from CPUEmulator's input stream", e);
        }
    }

    private void failWithError(Process cpuEmulator, String testFileName) {
        try (BufferedReader failureReader = new BufferedReader(new InputStreamReader(cpuEmulator.getErrorStream()))) {
            fail("Failure in running " + testFileName + ": " + failureReader.readLine());
        }
        catch (Exception e) {
            throw new RuntimeException("Could not read from CPUEmulator's error stream", e);
        }
    }
}
