package cppc.compiler.utils.globalnames;

public class CGlobalNames
  implements GlobalNames
{
  private static final String[] CHECKPOINT_PRAGMA = { "#pragma", "cppc", 
    "checkpoint" };
  private static final String[] CHECKPOINT_LOOP_PRAGMA = { "#pragma", "cppc", 
    "checkpoint", "loop" };
  private static final String[] REGISTER_PRAGMA = { "#pragma", "cppc", 
    "register" };
  private static final String[] UNREGISTER_PRAGMA = { "#pragma", "cppc", 
    "unregister" };
  private static final String[] BEGIN_EXECUTE_PRAGMA = { "#pragma", "cppc", 
    "execute", "on", "restart" };
  private static final String[] END_EXECUTE_PRAGMA = { "#pragma", "cppc", 
    "end", "execute" };
  private static final String[] SHUTDOWN_PRAGMA = { "#pragma", "cppc", 
    "shutdown" };
  private static final String CHECKPOINT_LABEL = "CPPC_RESTART_BLOCK_";
  
  public String[] CHECKPOINT_PRAGMA()
  {
    return CHECKPOINT_PRAGMA;
  }
  
  public String[] CHECKPOINT_LOOP_PRAGMA()
  {
    return CHECKPOINT_LOOP_PRAGMA;
  }
  
  public String[] REGISTER_PRAGMA()
  {
    return REGISTER_PRAGMA;
  }
  
  public String[] UNREGISTER_PRAGMA()
  {
    return UNREGISTER_PRAGMA;
  }
  
  public String[] BEGIN_EXECUTE_PRAGMA()
  {
    return BEGIN_EXECUTE_PRAGMA;
  }
  
  public String[] END_EXECUTE_PRAGMA()
  {
    return END_EXECUTE_PRAGMA;
  }
  
  public String[] SHUTDOWN_PRAGMA()
  {
    return SHUTDOWN_PRAGMA;
  }
  
  private static int checkpointCount = 0;
  private static final String ENTER_FUNCTION_LABEL = "CPPC_ENTER_FUNCTION";
  private static final String EXECUTE_LABEL = "CPPC_EXECUTE_BLOCK_";
  private static int executeCount = 0;
  private static final String EXIT_FUNCTION_LABEL = "CPPC_EXIT_FUNCTION";
  private static final String LOOP_UNROLLING_LABEL = "CPPC_LOOP_UNROLL_";
  private static int loopUnrollCount = 0;
  private static final String REGISTER_LABEL = "CPPC_REGISTER_BLOCK_";
  private static int registerCount = 0;
  private static final String UNREGISTER_LABEL = "CPPC_UNREGISTER_BLOCK_";
  private static int unregisterCount = 0;
  private static final String ADD_LOOP_INDEX_FUNCTION = "CPPC_Add_loop_index";
  private static final String CHECKPOINT_FUNCTION = "CPPC_Do_checkpoint";
  private static final String COMMIT_CALL_IMAGE_FUNCTION = "CPPC_Commit_call_image";
  private static final String CONTEXT_POP_FUNCTION = "CPPC_Context_pop";
  private static final String CONTEXT_PUSH_FUNCTION = "CPPC_Context_push";
  private static final String CREATE_CALL_IMAGE_FUNCTION = "CPPC_Create_call_image";
  private static final String INIT_CONFIGURATION_FUNCTION = "CPPC_Init_configuration";
  private static final String INIT_STATE_FUNCTION = "CPPC_Init_state";
  private static final String JUMP_NEXT_FUNCTION = "CPPC_Jump_next";
  private static final String REGISTER_DESCRIPTOR_FUNCTION = "CPPC_Register_descriptor";
  private static final String REGISTER_FOR_CALL_IMAGE_FUNCTION = "CPPC_Register_for_call_image";
  private static final String REGISTER_FUNCTION = "CPPC_Register";
  private static final String REMOVE_LOOP_INDEX_FUNCTION = "CPPC_Remove_loop_index";
  private static final String SET_LOOP_INDEX_FUNCTION = "CPPC_Set_loop_index";
  private static final String SHUTDOWN_FUNCTION = "CPPC_Shutdown";
  private static final String UNREGISTER_DESCRIPTOR_FUNCTION = "CPPC_Unregister_descriptor";
  private static final String UNREGISTER_FUNCTION = "CPPC_Unregister";
  
  public String CHECKPOINT_LABEL()
  {
    return "CPPC_RESTART_BLOCK_" + checkpointCount++;
  }
  
  public int CURRENT_CHKPT_CODE()
  {
    return checkpointCount;
  }
  
  public String ENTER_FUNCTION_LABEL()
  {
    return "CPPC_ENTER_FUNCTION";
  }
  
  public String EXECUTE_LABEL()
  {
    return "CPPC_EXECUTE_BLOCK_" + executeCount++;
  }
  
  public String EXIT_FUNCTION_LABEL()
  {
    return "CPPC_EXIT_FUNCTION";
  }
  
  public String LOOP_UNROLLING_LABEL()
  {
    return "CPPC_LOOP_UNROLL_" + loopUnrollCount++;
  }
  
  public String REGISTER_LABEL()
  {
    return "CPPC_REGISTER_BLOCK_" + registerCount++;
  }
  
  public String UNREGISTER_LABEL()
  {
    return "CPPC_UNREGISTER_BLOCK_" + unregisterCount++;
  }
  
  public String ADD_LOOP_INDEX_FUNCTION()
  {
    return "CPPC_Add_loop_index";
  }
  
  public String CHECKPOINT_FUNCTION()
  {
    return "CPPC_Do_checkpoint";
  }
  
  public String COMMIT_CALL_IMAGE_FUNCTION()
  {
    return "CPPC_Commit_call_image";
  }
  
  public String CONTEXT_POP_FUNCTION()
  {
    return "CPPC_Context_pop";
  }
  
  public String CONTEXT_PUSH_FUNCTION()
  {
    return "CPPC_Context_push";
  }
  
  public String CREATE_CALL_IMAGE_FUNCTION()
  {
    return "CPPC_Create_call_image";
  }
  
  public String INIT_CONFIGURATION_FUNCTION()
  {
    return "CPPC_Init_configuration";
  }
  
  public String INIT_STATE_FUNCTION()
  {
    return "CPPC_Init_state";
  }
  
  public String JUMP_NEXT_FUNCTION()
  {
    return "CPPC_Jump_next";
  }
  
  public String REGISTER_DESCRIPTOR_FUNCTION()
  {
    return "CPPC_Register_descriptor";
  }
  
  public String REGISTER_FOR_CALL_IMAGE_FUNCTION()
  {
    return "CPPC_Register_for_call_image";
  }
  
  public String REGISTER_FUNCTION()
  {
    return "CPPC_Register";
  }
  
  public String REMOVE_LOOP_INDEX_FUNCTION()
  {
    return "CPPC_Remove_loop_index";
  }
  
  public String SET_LOOP_INDEX_FUNCTION()
  {
    return "CPPC_Set_loop_index";
  }
  
  public String SHUTDOWN_FUNCTION()
  {
    return "CPPC_Shutdown";
  }
  
  public String UNREGISTER_DESCRIPTOR_FUNCTION()
  {
    return "CPPC_Unregister_descriptor";
  }
  
  public String UNREGISTER_FUNCTION()
  {
    return "CPPC_Unregister";
  }
  
  private static int NEXT_FILE_CODE = 0;
  
  public int NEXT_FILE_CODE()
  {
    return NEXT_FILE_CODE++;
  }
  
  private static String INCLUDE_FILE = "cppc.h";
  
  public String INCLUDE_FILE()
  {
    return INCLUDE_FILE;
  }
}
