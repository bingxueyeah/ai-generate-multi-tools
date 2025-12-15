package aitool.model;

/**
 * 诊断结果
 */
public class DiagnosticResult {
    public CheckResult configCheck;
    public CheckResult networkCheck;
    public CheckResult endpointCheck;
    public CheckResult authCheck;
    public String overallStatus;
    public String summary;
}
