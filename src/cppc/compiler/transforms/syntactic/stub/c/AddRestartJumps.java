package cppc.compiler.transforms.syntactic.stub.c;

import cetus.hir.ArrayAccess;
import cetus.hir.ArraySpecifier;
import cetus.hir.AssignmentExpression;
import cetus.hir.AssignmentOperator;
import cetus.hir.BinaryExpression;
import cetus.hir.BinaryOperator;
import cetus.hir.CompoundStatement;
import cetus.hir.Declarator;
import cetus.hir.Expression;
import cetus.hir.ExpressionStatement;
import cetus.hir.FunctionCall;
import cetus.hir.GotoStatement;
import cetus.hir.Identifier;
import cetus.hir.IfStatement;
import cetus.hir.Initializer;
import cetus.hir.IntegerLiteral;
import cetus.hir.PointerSpecifier;
import cetus.hir.Procedure;
import cetus.hir.Program;
import cetus.hir.Specifier;
import cetus.hir.UnaryExpression;
import cetus.hir.UnaryOperator;
import cetus.hir.VariableDeclaration;
import cetus.hir.VariableDeclarator;
import cppc.compiler.cetus.CppcConditionalJump;
import cppc.compiler.cetus.CppcLabel;
import cppc.compiler.utils.globalnames.GlobalNames;
import cppc.compiler.utils.globalnames.GlobalNamesFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AddRestartJumps
  extends cppc.compiler.transforms.syntactic.skel.AddRestartJumps
{
  private static final String CPPC_JUMP_POINTS_NAME = "cppc_jump_points";
  private static final String CPPC_JUMP_POINTS_SIZE_NAME = "cppc_jump_points_size";
  private static final String CPPC_JUMP_POINTS_CURRENT_NAME = "cppc_next_jump_point";
  
  private AddRestartJumps(Program program)
  {
    super(program);
  }
  
  public static final AddRestartJumps getTransformInstance(Program program)
  {
    return new AddRestartJumps(program);
  }
  
  protected void addCppcVariables(Procedure procedure, List<CppcLabel> orderedLabels)
  {
    int jumpPoints = orderedLabels.size();
    
    CompoundStatement statementList = procedure.getBody();
    
    ArrayList cppcJumpPointsDeclarationSpecs = new ArrayList();
    cppcJumpPointsDeclarationSpecs.add(Specifier.VOID);
    ArrayList cppcJumpPointsDeclaratorLeadingSpecs = new ArrayList();
    cppcJumpPointsDeclaratorLeadingSpecs.add(PointerSpecifier.UNQUALIFIED);
    ArrayList cppcJumpPointsDeclaratorTrailingSpecs = new ArrayList();
    cppcJumpPointsDeclaratorTrailingSpecs.add(new ArraySpecifier(
      new IntegerLiteral(jumpPoints)));
    Declarator cppcJumpPointsDeclarator = new VariableDeclarator(
      cppcJumpPointsDeclaratorLeadingSpecs, 
      new Identifier("cppc_jump_points"), 
      cppcJumpPointsDeclaratorTrailingSpecs);
    VariableDeclaration cppcJumpPointsDeclaration = new VariableDeclaration(
      cppcJumpPointsDeclarationSpecs, cppcJumpPointsDeclarator);
    statementList.addDeclaration(cppcJumpPointsDeclaration);
    
    ArrayList cppcJumpPointsSizeDeclarationSpecs = new ArrayList();
    cppcJumpPointsSizeDeclarationSpecs.add(Specifier.CONST);
    cppcJumpPointsSizeDeclarationSpecs.add(Specifier.INT);
    
    Declarator cppcJumpPointsSizeDeclarator = new VariableDeclarator(
      new Identifier("cppc_jump_points_size"));
    
    cppcJumpPointsSizeDeclarator.setInitializer(new Initializer(
      new IntegerLiteral(jumpPoints)));
    VariableDeclaration cppcJumpPointsSizeDeclaration = 
      new VariableDeclaration(cppcJumpPointsSizeDeclarationSpecs, 
      cppcJumpPointsSizeDeclarator);
    statementList.addDeclaration(cppcJumpPointsSizeDeclaration);
    
    Declarator cppcJumpPointsCurrentDeclarator = new VariableDeclarator(
      new Identifier("cppc_next_jump_point"));
    cppcJumpPointsCurrentDeclarator.setInitializer(new Initializer(
      new IntegerLiteral(0L)));
    VariableDeclaration cppcJumpPointsCurrentDeclaration = 
      new VariableDeclaration(Specifier.INT, 
      cppcJumpPointsCurrentDeclarator);
    statementList.addDeclaration(cppcJumpPointsCurrentDeclaration);
    
    ArrayList<UnaryExpression> orderedDirections = 
      new ArrayList(jumpPoints);
    Iterator<CppcLabel> labelsIter = orderedLabels.iterator();
    while (labelsIter.hasNext())
    {
      CppcLabel thisLabel = (CppcLabel)labelsIter.next();
      UnaryExpression labelDir = new UnaryExpression(
        UnaryOperator.LABEL_ADDRESS, 
        (Identifier)thisLabel.getName().clone());
      orderedDirections.add(labelDir);
    }
    Initializer initializer = new Initializer(orderedDirections);
    cppcJumpPointsDeclarator.setInitializer(initializer);
  }
  
  protected void addConditionalJump(CppcConditionalJump jump, List<CppcLabel> orderedLabels)
  {
    String CPPC_JUMP_INDEX_NAME = "jump_index";
    
    FunctionCall cppcJumpNextCall = new FunctionCall(
      new Identifier(
      GlobalNamesFactory.getGlobalNames().JUMP_NEXT_FUNCTION()));
    
    CompoundStatement jumpBody = new CompoundStatement();
    
    ArrayList<Specifier> jumpIndexSpecs = new ArrayList(2);
    jumpIndexSpecs.add(Specifier.CONST);
    jumpIndexSpecs.add(Specifier.INT);
    Declarator jumpIndexDeclarator = new VariableDeclarator(new Identifier(
      "jump_index"));
    
    Expression initExpression = new BinaryExpression(
      new Identifier("cppc_next_jump_point"), BinaryOperator.ADD, 
      new IntegerLiteral(jump.getLeap() - 1));
    jumpIndexDeclarator.setInitializer(new Initializer(initExpression));
    VariableDeclaration jumpIndexDeclaration = new VariableDeclaration(
      jumpIndexSpecs, jumpIndexDeclarator);
    jumpBody.addDeclaration(jumpIndexDeclaration);
    
    BinaryExpression increment = new BinaryExpression(
      new Identifier("jump_index"), BinaryOperator.ADD, 
      new IntegerLiteral(1L));
    
    BinaryExpression modulus = new BinaryExpression(increment, 
      BinaryOperator.MODULUS, new Identifier("cppc_jump_points_size"));
    
    AssignmentExpression assignment = new AssignmentExpression(
      new Identifier("cppc_next_jump_point"), 
      AssignmentOperator.NORMAL, modulus);
    
    ExpressionStatement assignmentStatement = new ExpressionStatement(
      assignment);
    jumpBody.addStatement(assignmentStatement);
    
    ArrayAccess arrayAccess = new ArrayAccess(new Identifier(
      "cppc_jump_points"), 
      new Identifier("jump_index"));
    
    UnaryExpression dereference = new UnaryExpression(
      UnaryOperator.DEREFERENCE, arrayAccess);
    
    dereference.setParens(false);
    
    GotoStatement gotoStatement = new GotoStatement(dereference);
    
    jumpBody.addStatement(gotoStatement);
    
    IfStatement conditionalJump = new IfStatement(cppcJumpNextCall, 
      jumpBody);
    
    jump.swapWith(conditionalJump);
  }
}
