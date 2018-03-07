package cppc.compiler.transforms.syntactic.skel;

import cetus.hir.*;


import cppc.compiler.transforms.shared.DataType;
import cppc.compiler.transforms.shared.TypeManager;
import cppc.compiler.transforms.shared.TypedefDataType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

// 检测用户自定义类型  typedef  
//
public class DetectUserTypes
{
  private static String passName = "[DetectUserTypes]";
  private Program program;
  
  private DetectUserTypes(Program program)
  {
    this.program = program;
  }
  
  public static void run(Program program)
  {
    //Tools.printlnStatus(passName + " begin", 1);
    //Tools.printlnStatus(passName + " end", 1);


    double timer = Tools.getTime();
    PrintTools.println(passName + " begin", 0);

    DetectUserTypes transform = new DetectUserTypes(program);
    transform.start();
    
    PrintTools.println(passName + " end in " + String.format("%.2f seconds", Tools.getTime(timer)), 0);


  }
  
  private void start()
  {
    DepthFirstIterator programIter = new DepthFirstIterator(this.program);
    programIter.pruneOn(TranslationUnit.class);
    programIter.next();
    while (programIter.hasNext()) {//便历所有文件
      try{
        
        TranslationUnit tunit = (TranslationUnit)programIter.next(TranslationUnit.class);
        
        DepthFirstIterator tunitIter = new DepthFirstIterator(tunit);
        tunitIter.pruneOn(Procedure.class);
        tunitIter.next();

        while (tunitIter.hasNext()) {//遍历所有函数
          try{
            
            DeclarationStatement s = (DeclarationStatement)tunitIter.next(DeclarationStatement.class);//取声明语句
            
            if ((s.getDeclaration() instanceof VariableDeclaration)){//变量声明

              VariableDeclaration decl = (VariableDeclaration)s.getDeclaration();

              if (((Specifier)decl.getSpecifiers().get(0)).equals(Specifier.TYPEDEF)){ //首个标识符是typedef
                int arraySize = decl.getSpecifiers().size();
                
                List<Specifier> specs = decl.getSpecifiers().subList(1, arraySize);//左闭右开，去掉typedef
                
                if (TypeManager.isRegistered(specs)){// 必须是注册过得
                  Identifier baseType = TypeManager.getType(specs).getBaseType();//得到基础类型
                  
                  if ((decl.getDeclarator(0) instanceof VariableDeclarator)){//变量名验证
                    
                    VariableDeclarator vdecl = (VariableDeclarator)decl.getDeclarator(0);//取出变量名
                    
                    Identifier symbol = (Identifier)vdecl.getID(); //getSymbol();替换掉
                    List<ArraySpecifier> arraySpecs = vdecl.getArraySpecifiers();
                    int size = 1;
                    
                    if (!arraySpecs.isEmpty()){ //非空
                      //Iterator iter = arraySpecs.iterator();
                      //ArraySpecifier aspec;
                      //int i;
                      
                      for(ArraySpecifier aspec : arraySpecs ){
                        for( int i = 0 ;i< aspec.getNumDimensions() ;i++ ){
                          IntegerLiteral dimSize = (IntegerLiteral)aspec.getDimension(i);
                          size *= (int)dimSize.getValue();
                        }
                      }

                      //for (; iter.hasNext(); )//i < aspec.getNumDimensions() )
                      //{
                        
                        //aspec = (ArraySpecifier)iter.next();
                      //  i = 0; 
                      //  continue;
                        //IntegerLiteral dimSize = (IntegerLiteral)aspec.getDimension(i);
                        //size = (int)(size * dimSize.getValue());
                      //  i++;
                      //}
                    }

                    UserSpecifier uspec = new UserSpecifier((Identifier)symbol.clone());
                    
                    specs = new ArrayList<Specifier>(1);
                    specs.add((Specifier)uspec);
                    TypeManager.addType(specs, new TypedefDataType(baseType, size));
                  }
                }



              }


            }




          }
          catch (NoSuchElementException localNoSuchElementException) {}
        }
      }
      catch (NoSuchElementException localNoSuchElementException1) {}
    }

    
  }




}
