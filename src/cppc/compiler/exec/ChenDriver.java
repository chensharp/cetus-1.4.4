package cppc.compiler.exec;

import java.io.*;
import java.util.*;

import cetus.analysis.*;
import cetus.hir.*;
import cetus.transforms.*;
import cetus.codegen.*;
import cetus.exec.*;

import cppc.compiler.analysis.ChenAnalysis;

import cppc.compiler.transforms.semantic.skel.AddCppcExecutePragmas;
import cppc.compiler.transforms.semantic.skel.AddCppcInitPragma;
import cppc.compiler.transforms.semantic.skel.AddOpenFilesControl;
import cppc.compiler.transforms.semantic.skel.CommunicationMatching;
import cppc.compiler.transforms.syntactic.skel.AddCheckpointPragmas;
import cppc.compiler.transforms.syntactic.skel.AddCppcShutdownPragma;
import cppc.compiler.transforms.syntactic.skel.AddExitLabels;
import cppc.compiler.transforms.syntactic.skel.AddLoopContextManagement;
import cppc.compiler.transforms.syntactic.skel.AddRestartJumps;
import cppc.compiler.transforms.syntactic.skel.CheckCheckpointedProcedures;
import cppc.compiler.transforms.syntactic.skel.CheckPragmedProcedures;
import cppc.compiler.transforms.syntactic.skel.CppcDependenciesAnalizer;
import cppc.compiler.transforms.syntactic.skel.CppcStatementFiller;
import cppc.compiler.transforms.syntactic.skel.CppcStatementsToStatements;
import cppc.compiler.transforms.syntactic.skel.DetectUserTypes;
import cppc.compiler.transforms.syntactic.skel.EnterPragmedProcedures;
import cppc.compiler.transforms.syntactic.skel.LanguageTransforms;
import cppc.compiler.transforms.syntactic.skel.ManualPragmasToCppcPragmas;
import cppc.compiler.transforms.syntactic.skel.PragmaDetection;
import cppc.compiler.transforms.syntactic.skel.RequireProcedureReturns;
import cppc.compiler.transforms.syntactic.skel.StatementsToCppcStatements;
import cppc.compiler.utils.ConfigurationManager;





/**
 * <b>chenDriver </b> implements the command line parser and controls pass ordering.
 * Users may extend this class by overriding runPasses
 * (which provides a default sequence of passes).  The derived
 * class should pass an instance of itself to the run method.
 * Derived classes have access to a protected {@link Program Program} object.
 * 
 * @author chensharp <chensharps@163.com>
 * 
 *         School of Software Engineering, Xi'an JiaoTong University
 */
public class ChenDriver extends Driver
{

	private static final String C_ADDCPPCINITPRAGMA_CLASSNAME          = "cppc.compiler.transforms.semantic.stub.c.AddCppcInitPragma";
	private static final String C_ADDFILESCONTROL_CLASSNAME            = "cppc.compiler.transforms.semantic.stub.c.AddOpenFilesControl";

	private static final String C_ADDLOOPCONTEXTMANAGEMENT_CLASSNAME   = "cppc.compiler.transforms.syntactic.stub.c.AddLoopContextManagement";
	private static final String C_ADDRESTARTJUMPS_CLASSNAME            = "cppc.compiler.transforms.syntactic.stub.c.AddRestartJumps";
	private static final String C_CODEOPTIMIZATIONS_CLASSNAME          = "cppc.compiler.transforms.syntactic.stub.c.CodeOptimizations";
	private static final String C_LANGUAGETRANSFORMS_CLASSNAME         = "cppc.compiler.transforms.syntactic.stub.c.LanguageTransforms";
	private static final String C_PRAGMADETECTION_CLASSNAME            = "cppc.compiler.transforms.syntactic.stub.c.PragmaDetection";

	private static final String C_COMMUNICATIONANALYZER_CLASSNAME      = "cppc.compiler.analysis.CCommunicationAnalyzer";
	private static final String C_EXPRESSIONANALYZER_CLASSNAME         = "cppc.compiler.analysis.CExpressionAnalyzer";
	private static final String C_SPECIFIERANALYZER_CLASSNAME          = "cppc.compiler.analysis.CSpecifierAnalyzer";
	private static final String C_STATEMENTANALYZER_CLASSNAME          = "cppc.compiler.analysis.CStatementAnalyzer";
	private static final String C_SYMBOLICANALYZER_CLASSNAME           = "cppc.compiler.analysis.CSymbolicAnalyzer";
	private static final String C_SYMBOLICEXPRESSIONANALYZER_CLASSNAME = "cppc.compiler.analysis.CSymbolicExpressionAnalyzer";

	private static final String C_VARIABLESIZEANALYZER_CLASSNAME       = "cppc.compiler.utils.CVariableSizeAnalizer";
	private static final String C_GLOBALNAMES_CLASSNAME                = "cppc.compiler.utils.globalnames.CGlobalNames";
	private static final String C_LANGUAGEANALYZER_CLASSNAME           = "cppc.compiler.utils.language.CLanguageAnalyzer";


	static
	{
		//这些用来取出类名。

		ConfigurationManager.setOption(
			"CPPC/Transforms/AddCppcInitPragma/ClassName", 
			"cppc.compiler.transforms.semantic.stub.c.AddCppcInitPragma");

		ConfigurationManager.setOption(
			"CPPC/Transforms/AddLoopContextManagement/ClassName", 
			"cppc.compiler.transforms.syntactic.stub.c.AddLoopContextManagement");

		ConfigurationManager.setOption(
			"CPPC/Transforms/AddOpenFilesControl/ClassName", 
			"cppc.compiler.transforms.semantic.stub.c.AddOpenFilesControl");

		ConfigurationManager.setOption("CPPC/Transforms/AddRestartJumps/ClassName", 
			"cppc.compiler.transforms.syntactic.stub.c.AddRestartJumps");

		ConfigurationManager.setOption("CPPC/Utils/GlobalNames/ClassName", 
			"cppc.compiler.utils.globalnames.CGlobalNames");

		ConfigurationManager.setOption("CPPC/Transforms/CodeOptimizations/ClassName", 
			"cppc.compiler.transforms.syntactic.stub.c.CodeOptimizations");

		ConfigurationManager.setOption(
			"CPPC/Analysis/CommunicationAnalyzer/ClassName", 
			"cppc.compiler.analysis.CCommunicationAnalyzer");

		ConfigurationManager.setOption(
			"CPPC/Analysis/ExpressionAnalyzer/ClassName", 
			"cppc.compiler.analysis.CExpressionAnalyzer");

		ConfigurationManager.setOption("CPPC/Utils/LanguageAnalyzer/ClassName", 
			"cppc.compiler.utils.language.CLanguageAnalyzer");

		ConfigurationManager.setOption(
			"CPPC/Utils/LanguageTransforms/ClassName", 
			"cppc.compiler.transforms.syntactic.stub.c.LanguageTransforms");

		ConfigurationManager.setOption("CPPC/Transforms/PragmaDetection/ClassName", 
			"cppc.compiler.transforms.syntactic.stub.c.PragmaDetection");

		ConfigurationManager.setOption("CPPC/Analysis/SpecifierAnalyzer/ClassName", 
			"cppc.compiler.analysis.CSpecifierAnalyzer");

		ConfigurationManager.setOption("CPPC/Analysis/StatementAnalyzer/ClassName", 
			"cppc.compiler.analysis.CStatementAnalyzer");

		ConfigurationManager.setOption("CPPC/Analysis/SymbolicAnalyzer/ClassName", 
			"cppc.compiler.analysis.CSymbolicAnalyzer");

		ConfigurationManager.setOption(
			"CPPC/Analysis/SymbolicExpressionAnalyzer/ClassName", 
			"cppc.compiler.analysis.CSymbolicExpressionAnalyzer");

		ConfigurationManager.setOption(
			"CPPC/Utils/VariableSizeAnalyzer/ClassName", 
			"cppc.compiler.utils.CVariableSizeAnalizer");

	}







	protected ChenDriver() {
		super();
		
		options.add("chen",
			"Generate a new program from MPI program");
	    
		//Add Cuda-Specific command-line options.
		//options.add("doNotUseCUTIL",
		//"Generate CUDA code without using cutil tools.");
		
	}

	/**
	 * Runs this driver with args as the command line.
	 *
	 * @param args The command line from main.
	 */
	public void run(String[] args)
	{
		parseCommandLine(args);//deal with comandline
		//parseCudaConfFile();
		//HashMap<String, HashMap<String,Object>> userDirectives = parseCudaUserDirectiveFile();
		//HashMap<String, Object> tuningConfigs = parseTuningConfig();




		parseFiles();// deal with code files

		if (getOptionValue("parse-only") != null)
		{
			System.err.println("parsing finished and parse-only option set");
			System.exit(0);
		}
		

		runPasses();//run circle for passes,note!
		
	    	//if (getOptionValue("chen") != null)//generate target code
	    	//{
	    		//CodeGenPass.run(new omp2gpu(program, userDirectives, tuningConfigs));
	    	//}

		PrintTools.printlnStatus("Printing...", 1);

		try {
			program.print();//print result code to the files
		} catch (IOException e) {
			System.err.println("could not write output files: " + e);
			System.exit(1);
		}
	}
	/**
	 * Entry point for Cetus; creates a new Driver object,
	 * and calls run on it with args.
	 *
	 * @param args Command line options.
	 */
	public static void main(String[] args)
	{
		/* Set default options for omp2gpu translator. */
		ChenDriver chendriver = new ChenDriver();
		setOptionValue("chen", "1");
		System.err.println("welcome to chen.driver ");

		//第一步处理参数，
		args = ConfigurationManager.parseCommandLine(args);

		chendriver.run(args);
	}

	/**
	* write some passes , important!
    *
	*/
	public void runPasses(){

		//run chen analysis,for test .
		AnalysisPass.run(new ChenAnalysis(program));//自定义的pass示例

		DetectUserTypes.run(program);//检测用户自定义类型 ，ok

		ManualPragmasToCppcPragmas.run(this.program);//手动参数转换为cppc参数，
		
		AddCppcInitPragma.run(this.program);//添加初始化函数，
		
		//AddCppcShutdownPragma.run(this.program);
		
		//AddCppcExecutePragmas.run(this.program);
		
		//AddOpenFilesControl.run(this.program);
		
		//StatementsToCppcStatements.run(this.program); //

		//CppcStatementFiller.run(this.program);  //

		//CommunicationMatching.run(this.program); // 通信匹配，重要

		//AddCheckpointPragmas.run(this.program); //

		//LanguageTransforms.run(this.program); //

		//CheckCheckpointedProcedures.run(this.program);
		
		//CppcDependenciesAnalizer.run(this.program);   //依赖分析 ，

		//CppcStatementsToStatements.run(this.program);

		//CheckPragmedProcedures.run(this.program);

		//EnterPragmedProcedures.run(this.program);

		//AddLoopContextManagement.run(this.program);

		//PragmaDetection.run(this.program);

		//AddExitLabels.run(this.program);//添加退出标签

		//AddRestartJumps.run(this.program);//添加重启动标签

		//RequireProcedureReturns.run(this.program);




	}
	
	
}

