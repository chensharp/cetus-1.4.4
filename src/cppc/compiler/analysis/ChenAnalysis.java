package cppc.compiler.analysis;

import cetus.analysis.*;
import cetus.exec.Driver;
import cetus.hir.*;

import java.util.*;

/**
 * chen for test 
 */
public class ChenAnalysis extends AnalysisPass
{
    /** Pass tag */
    private static final String tag = "[ChenAnalysis]";
    /** Set of tractable expression types */
    private static final Set<Class<? extends Expression>> tractable_class;
    /** Set of tractable operation types */
    private static final Set<Printable> tractable_op;
    /** Debug level */
    private static int debug;

    /** Option for disabling range computation */
    public static final int RANGE_EMPTY = 0;
    /** Option for enforcing intra-procedural analysis */
    public static final int RANGE_INTRA = 1;
    /** Option for enforcing inter-procedural analysis */
    public static final int RANGE_INTER = 2;
    /** Option for enforcing use of range pragma and constraint */
    public static final int RANGE_PRAGMA = 3; // TODO: publishable?
    /** Range analysis option */
    private static int option;
    /** Read-only Literals */
    private static final IntegerLiteral one = new IntegerLiteral(1);

    static {
        tractable_class = new HashSet<Class<? extends Expression>>();
        tractable_class.add(ArrayAccess.class);
        tractable_class.add(BinaryExpression.class);
        tractable_class.add(Identifier.class);
        tractable_class.add(InfExpression.class);
        tractable_class.add(IntegerLiteral.class);
        tractable_class.add(MinMaxExpression.class);
        tractable_class.add(RangeExpression.class);
        tractable_class.add(UnaryExpression.class);
        tractable_op = new HashSet<Printable>();
        tractable_op.add(BinaryOperator.ADD);
        tractable_op.add(BinaryOperator.DIVIDE);
        tractable_op.add(BinaryOperator.MODULUS);
        tractable_op.add(BinaryOperator.MULTIPLY);
        tractable_op.add(UnaryOperator.MINUS);
        tractable_op.add(UnaryOperator.PLUS);
        debug = PrintTools.getVerbosity();
        option = RANGE_INTRA;
        if (Driver.getOptionValue("range") != null) {
            option = Integer.parseInt(Driver.getOptionValue("range"));
        }
    }

    /**
    * Interprocedural input.
    * ip_node: the procedure node to be processed.
    * ip_cfg : the procedure control flow graph to be processed.
    */
    private static IPANode ip_node = null;
    private static CFGraph ip_cfg = null;

    /** Result of interprocedural range analysis. */
    private static Map<Procedure, Map<Statement, RangeDomain>> ip_ranges = null;

    /** Alias analysis for less conservative range analysis. */
    private static AliasAnalysis alias;

    /**
    * Past result of range analysis kept in a single map which needs
    * invalidation if a transformation pass is called
    */
    private static final Map<Statement, RangeDomain> range_domains =
            new HashMap<Statement, RangeDomain>();
    private static final RangeDomain empty_range = new RangeDomain();

    // TODO: experimental code
    private static final Set<String> safe_functions = new HashSet<String>();
    static {
        String s = System.getenv("CETUS_RANGE_SAFE_FUNCTIONS");
        if (s != null) {
            for (String safe_function : s.split(",")) {
                safe_functions.add(safe_function);
            }
        }
    }

    /**
    * Constructs a range analyzer for the program. The instantiated constructor
    * is used only internally.
    * @param program  the input program.
    */
    public ChenAnalysis(Program program) {
        super(program);
    }

    /**
    * Returns the pass name.
    * @return the pass name in string.
    */
    @Override
    public String getPassName() {
        return tag;
    }

    /**
    * Starts range analysis for the whole program. this is useful only for
    * testing the range analysis.
    */
    @Override
    public void start() {
        double timer = Tools.getTime();

        DFIterator<Procedure> iter =
                new DFIterator<Procedure>(program, Procedure.class);
        iter.pruneOn(Procedure.class);
        iter.pruneOn(Declaration.class);
        //while (iter.hasNext()) {
          //  Procedure procedure = iter.next();
            //Map<Statement, RangeDomain> ranges = getRanges(procedure);
            //addAssertions(procedure, ranges);
            // Test codes for tools with range information.
            //testSubstituteForward((Procedure)o, range_map);
        //}

        DFIterator<TranslationUnit> titer = new DFIterator<TranslationUnit>(program, TranslationUnit.class);
        titer.pruneOn(TranslationUnit.class);
        while (titer.hasNext()) {
            TranslationUnit tu = titer.next();
            Declaration ref = (Declaration)tu.getChildren().get(0);
            tu.addDeclarationBefore(ref, new AnnotationDeclaration(
                    new CodeAnnotation("#include <assert.h>")));
        }
        timer = Tools.getTime(timer);
        PrintTools.printlnStatus(1, tag, String.format("%.2f\n", timer));
    }
/*
    // Adds assert() call for range validation.
    private void addAssertions( Procedure procedure, Map<Statement, RangeDomain> ranges) {
        DFIterator<Statement> iter = new DFIterator<Statement>(procedure, Statement.class);
        while (iter.hasNext()) {
            Statement stmt = iter.next();
            
            if (stmt instanceof DeclarationStatement ||
                stmt instanceof CompoundStatement &&
                    !(stmt.getParent() instanceof CompoundStatement)) {
                continue;
            } // Skip program point that doesn't have a state.
            RangeDomain rd = ranges.get(stmt);
            if (rd != null && rd.size() > 0) {
                Expression test = rd.toExpression();
                stmt.annotateBefore(
                        new CodeAnnotation("assert(" + test + ");"));
            }
        }
    }
*/
  
   


   

    

  


}
