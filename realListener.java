//��������
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;


import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.semantics.SymbolChecks;

class Symtable{
	public Map<String, String> map;  //������Ŀ��������:������
	public ArrayList<Symtable> children;  //���ӽڵ�
	public Map <String, Integer > index; //ӳ�亯����,������:��children���±�
	public Symtable parent;  //���ױ�
	public String tablename;  //�����֣����������֣��Ǻ���,�����飬for��if�ȶ���������,���±����
	public String curtype;  //��ǰ������Ŀ������

	//ÿ�ν���һ��blockģ��ʱ���ڱ��ڵ㽨��һ���µ�block�����������block��
	//�����ַ��Ŷ���ģ��ʱ����������ӵ���block��;��������ʶ��ʱ�������block��ʼѰ�ң�һֱ�ҵ�ȫ��block;
	//���û�У���ôû�ж��壻��������ڱ�block�ҵ����壬��ô�ظ����壻����Ǻ�������ô�������κεط��ظ�����
	
	public Symtable(String name) {
		map= new HashMap<String,String>();
		index =new HashMap<String,Integer>();
		children=new ArrayList<Symtable>();
	
		tablename=name;
	}
	
	public void addEntry(String key,String val) {
		//key:������,val:��������
		if(!map.containsKey(key)) {
			//index.put(key, children.size());д�����ˡ���
			map.put(key, val);
		}
		else  if(map.get(key)=="null") {
			map.put(key, val);
		}
		else {
			System.out.println("already exit the Identifier!key:"+key+" val:" + map.get(key));
		}	
	}
	public void addTable(Symtable newtable) {
		newtable.parent=this;
		children.add(newtable);
		index.put(newtable.tablename,children.size()-1);
	}
	static public void tra(Symtable now,int depth) {
		System.out.println("In depth"+depth+"->Travel the Table: "+now.tablename);
		Iterator<Map.Entry<String, String>> entries = now.map.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry<String, String> entry = entries.next();
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
		}

		for(int i=0;i<now.children.size();i++) {
			tra(now.children.get(i) ,depth+1);
		}
	}
	static public boolean checkMissingId(Symtable cur,String ctx) {
		while(cur!=null) {
			if(cur.map.containsKey(ctx)) {
				return false;
			}
			cur=cur.parent;
		}
		return true;
	}
	static public boolean checkRedec(Symtable cur,String ctx) {
		return cur.map.containsKey(ctx);
	}
}

class FuncSymtable extends Symtable{
    boolean isdefined;
	public FuncSymtable(String name,boolean defined) {
		super(name);
		isdefined=defined; //����Ϊfalse������Ϊtrue
	}
	
}

class MyNode {
    public String typename;  //ԭ�ȵĹ�������
    public String operation;  //��Ϊast�ڵ�Ĳ�����,ʵ����ast�Ĳ�����operation����ע��
	public MyNode parent;
	public ArrayList<MyNode> children ;
	public String funcname; //һ��ʼ��Ƶ�ʱ������ˣ�����IRҪ�����������
	public MyNode() {
		typename="";
		children = new ArrayList<MyNode>();
		parent=null;
	}
	public MyNode(String name) {
		typename=name;
		children=new ArrayList<MyNode>();
		parent=null;
	}
	public MyNode(String name, String op) {
		typename=name;
		children=new ArrayList<MyNode>();
		parent=null;
		operation=op;
	}
	public void addchild(MyNode son) {
		son.parent=this;
		children.add(son);
	}
	static public void Tra(MyNode root,int depth) { //�������
		if(root.typename=="")
			return ;
		for(int i=0;i<depth;i++) {
			System.out.print("----$ ");
		}
		System.out.println(root.typename+" "+root.operation);
		
		for(int i=0;i<root.children.size();i++) {
			Tra(root.children.get(i) ,depth+1);
		}
	}
	static public void transfer(MyNode dst,MyNode src) {
		MyNode tmp;
		for(int i=0;i<src.children.size();i++) {
			tmp=src.children.get(i);
			tmp.parent=dst;
			dst.children.add(tmp);
		}
	}
}
public class realListener extends CBaseListener{
	Stack<MyNode> store;
	MyNode rootnode;
	Symtable rootTable;  //���ű���ڵ�
	Symtable curTable;   //��ǰ��
	
	//����Ϊ�������̿��Ʋ��֣�����˳��ִ�����顢forѭ����if-else����ת��ȵ�
	@Override public void enterCompilationUnit(@NotNull CParser.CompilationUnitContext ctx) {
		store=new Stack<MyNode>();
		rootnode=new MyNode("start");
		rootnode.operation="start";
		store.add(rootnode);
		
		rootTable=new Symtable("Root");
		rootTable.parent=null;
		curTable=rootTable;
	}

	@Override public void exitCompilationUnit(@NotNull CParser.CompilationUnitContext ctx) {
		MyNode result=store.pop();
		if(result.typename=="start") {
			System.out.println("I am traveling the AST!");
			
			MyNode.Tra(result, 0);
			if(curTable==null)
				System.out.println("Table Error!");
			System.out.println("\n\nI am traveling the table!");
			Symtable.tra(curTable, 0);
		}else {		
			System.out.println("Error!:The node in the stack is "+result.typename+" op: "+result.operation);
			while(!store.empty()) {
				result=store.pop();
				System.out.println("Error!\nThe node in the stack is "+result.typename+" op: "+result.operation);
			}
		}
		IrGenerator IR=new IrGenerator(rootTable);
		System.out.println("\n\nGenerating IR!");
		IR.Generate(result);
	}

	//����Ϊblockitem������ջ��ֻ��Ҫ��blockitemlist��statement��ջ������
	@Override public void enterBlockItemList(@NotNull CParser.BlockItemListContext ctx) {
		MyNode tmpNode=new MyNode("Blocklist","Blocklist");
		store.add(tmpNode);
		
	}

	@Override public void exitBlockItemList(@NotNull CParser.BlockItemListContext ctx) { 
		MyNode result=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Deffunc" && father.operation=="Deffunc") {
			//�������壬���ݽڵ������Deffunc;�о��Ǵ��ݺ�
			MyNode.transfer(father, result);
			//father.addchild(result);
		}
		else if(father.typename=="Selection") {
			//��Ϊif����else�����飬�����Լ�,�о��ǹ��غ�
			//MyNode.transfer(father, result);
			//System.out.println("Father is selection!");
			father.addchild(result);
		}
		else if(father.typename=="Loop" ) {
			if(father.operation=="forloop") {
				int child=father.children.size();
				for(int i=0;i<2-child;i++) {
					father.addchild(new MyNode("Loop","forloop"));
				}
				father.addchild(result);
			}
			else if(father.typename=="Loop" && father.operation=="fordeclar") { //
				store.pop();
				father=store.peek(); //����һ���ڵ�
				for(int i=0;i<2;i++) {
					father.addchild(new MyNode("Loop","forloop"));
				}
			}

		}
		else if (father.typename=="Blocklist") {
			MyNode.transfer(father, result);
		}
	}
	
	@Override public void enterExpression(@NotNull CParser.ExpressionContext ctx) {
		MyNode father=store.peek();
		if(father.typename=="Selection" ) {//if_single��if_else
			//�ر���Ϊif��������䵥��
			MyNode tmpNode=new MyNode("Exp","ifcondition"); //������
			store.add(tmpNode);
		}
		else { //��ͨ������
			MyNode tmpNode=new MyNode("Exp","Exp");
			store.add(tmpNode);
		}
		
	}
	
	@Override public void exitExpression(@NotNull CParser.ExpressionContext ctx) {
	
		MyNode myNode=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Selection") {
			//��������ص�if�ڵ�,Ӧ��ֻ������Ϊif������
			father.addchild(myNode);
		}
		else if(father.typename=="Blocklist") {
			//���ݸ�blocklist�����ǲ�����exp����
			MyNode.transfer(father, myNode);
		}
		else if(father.typename=="Exp"&& father.operation=="Primary"){ 
			//���������ű��ʽ��һ����ʽ���ݸ�blockitemlist
			MyNode.transfer(father, myNode);
		}
		else if(father.typename=="Return" && father.operation=="return") {
			MyNode.transfer(father, myNode);
		}
		//Ҫ����exp��Ϊwhile��for��������һ������
	}
//����if�����̿��Ʋ���:
	@Override public void enterIf_single(@NotNull CParser.If_singleContext ctx) { 
		MyNode tmpNode=new MyNode("Selection","if_single");//if_else�Ŀ�ͷ
		store.add(tmpNode);
	}
	
	@Override public void exitIf_single(@NotNull CParser.If_singleContext ctx) {
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Selection") {  
			if(father.operation=="else") {//��if���ص�����else��,��ֱ�Ӹ���statementӦ�ò�������
				MyNode.transfer(father, tmpNode);
			}
		}
		else if(father.typename=="Blocklist") {
			//����if���ص�blocklist֮��
			father.addchild(tmpNode);
		}
	}
	
//if-else���̿��Ʋ���:
	@Override public void enterIf_else(@NotNull CParser.If_elseContext ctx) {
		MyNode tmpNode=new MyNode("Selection","if_else");//if_else�Ŀ�ͷ
		store.add(tmpNode);
	}

	@Override public void exitIf_else(@NotNull CParser.If_elseContext ctx) {
		//������׽ڵ���else����ô��ת�ƣ�������أ�
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Selection") {  
			if(father.operation=="else") {//��if���ص�����else��,��ֱ�Ӹ���statementӦ�ò�������
				MyNode.transfer(father, tmpNode);
			}
		}
		else if(father.typename=="Blocklist") {
			//����if���ص�blocklist֮��
			father.addchild(tmpNode);
		}
	}
	@Override public void enterElseStatement(@NotNull CParser.ElseStatementContext ctx) { 
		MyNode tmpNode=new MyNode("Selection","else");
		store.add(tmpNode);
	}


	@Override public void exitElseStatement(@NotNull CParser.ElseStatementContext ctx) {
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Selection") { 
			father.addchild(tmpNode); //�Ѷ���ת�Ƹ�if���
		}
		else {
			System.out.println("Expect selection but not!-->"+father.typename);
		}
	}
	
//forѭ������
	@Override public void enterForloop(@NotNull CParser.ForloopContext ctx) {
		MyNode tmpNode=new MyNode("Loop","forloop");//forѭ���Ŀ�ͷ
		store.add(tmpNode);
		//ע�⣬��for��block����֮ǰ����Ҫ�۲�for�м������ӣ�д��blocklist����
	}

	@Override public void exitForloop(@NotNull CParser.ForloopContext ctx) {
		//forѭ��ֱ�ӹ��ص����׽ڵ�
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpNode);
	}
	@Override public void enterForDeclaration(@NotNull CParser.ForDeclarationContext ctx) {
		MyNode tmpNode=new MyNode("Loop","fordeclar");//forѭ���Ŀ�ͷ
		store.add(tmpNode);
		Symtable tmptable=new Symtable("forloop");
		curTable.addTable(tmptable);
		curTable=tmptable;
		curTable.curtype=ctx.typespecifier().getText();
	}
	@Override public void exitForDeclaration(@NotNull CParser.ForDeclarationContext ctx) {
		//curTable=curTable.parent; �ƺ�compoundstate�����
		store.pop();
	}
	@Override public void enterInitequ(@NotNull CParser.InitequContext ctx) {
		MyNode father=store.peek();
		if(father.typename=="Loop" && father.operation=="fordeclar")
			curTable.addEntry(ctx.directDeclarator().getText(), curTable.curtype);
	}
	@Override public void enterInitdirec(@NotNull CParser.InitdirecContext ctx) {
		MyNode father=store.peek();
		if(father.typename=="Loop" && father.operation=="fordeclar")
			curTable.addEntry(ctx.directDeclarator().getText(), curTable.curtype);
	}
	@Override public void enterForExpression(@NotNull CParser.ForExpressionContext ctx) {
		
		MyNode tmpNode=new MyNode("Loop","forconditon");//forѭ���Ŀ�ͷ
		store.add(tmpNode);
	}
	
	@Override public void exitForExpression(@NotNull CParser.ForExpressionContext ctx) {
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Loop" && father.operation=="fordeclar") { //
			store.pop();
			father=store.peek(); //����һ���ڵ�
			father.addchild(new MyNode("Loop","forcondition")); //��ʾcondition�ǿ�
		}
		//�ǿ����ò��Ͽ�;ע�����ﲻ�Ǵ�exp������
		MyNode.transfer(father, tmpNode);	
	}
	//ע�⣬���forcondition��Ҫ�ƶ��Լ��ǵ�һ�����ǵڶ��������ǰһ��Ϊ��
	
//return����:
	@Override public void enterReturn(@NotNull CParser.ReturnContext ctx) {
		MyNode tmpNode=new MyNode("Return","return");//if_else�Ŀ�ͷ
		store.add(tmpNode);
	}
	
	@Override public void exitReturn(@NotNull CParser.ReturnContext ctx) {
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpNode);
	}
//goto����	
	@Override public void enterLabel(@NotNull CParser.LabelContext ctx) {
		//�����ʶ��
		curTable.addEntry(ctx.Identifier().getText(), "JumpLabel");
		MyNode tmpNode=new MyNode("JumpLabel",ctx.Identifier().getText());//����IR�����Ҳ�����
		MyNode father=store.peek();
		father.addchild(tmpNode);
	}
	@Override public void enterGoto(@NotNull CParser.GotoContext ctx) {
		//��ת����ʶ��
		MyNode tmpNode=new MyNode("Goto",ctx.Identifier().getText());
		MyNode father=store.peek();
		father.addchild(tmpNode);
	}
//break��continue;
	@Override public void enterBreak(@NotNull CParser.BreakContext ctx) {
		MyNode son=new MyNode("break","break");
		MyNode father=store.peek();
		father.addchild(son);
	}
	@Override public void enterContinue(@NotNull CParser.ContinueContext ctx) { 
		MyNode son=new MyNode("continue","continue");
		MyNode father=store.peek();
		father.addchild(son);
	}
	//����Ϊ���塢��������
	@Override public void enterDeclaration(@NotNull CParser.DeclarationContext ctx) {
		//�����������߱������������
		MyNode tmpMyNode=new MyNode("Declar");  
		tmpMyNode.operation="Declar";
		store.add(tmpMyNode);
		//curTable.curtype=ctx.typespecifier().getText(); //������ǰ����������
		//System.out.println(curTable.curtype);
	}
		

	@Override public void exitDeclaration(@NotNull CParser.DeclarationContext ctx) {
		store.pop();
	}
	
	@Override public void enterDirecid(@NotNull CParser.DirecidContext ctx) { //��ָdirectDeclarator ->id��� 
		//�����ı�ʶ�����֣�����һ�����������ͺ�������
		//����Ǻ������壬��ô��Ҫ��дtable!
		//����Ǻ�����������ô���ǵ�һ��д�������������ظ�������
		//�����������������ظ�����
		String curtext=ctx.getText();
		//System.out.println(ctx.getText());
		if(store.peek().typename=="Declar") {
			//�����������ߺ�����������
			if(!Symtable.checkRedec(curTable,curtext)) {
				if(store.peek().operation=="Func") { //��������������
					curTable.addEntry(curtext, "Function");
					store.peek().funcname=curtext;
					FuncSymtable newtable=new FuncSymtable(curtext,false);
					curTable.addTable(newtable);
					curTable=newtable;
				}
				else if(store.peek().operation=="Array")  //����һ������ok
					curTable.addEntry(curtext, "Array");
				else   //����һ����ͨ������Ӧ��Ҳ��ok��
					curTable.addEntry(curtext, curTable.curtype);
			}
			else {
				System.out.println("Error2!Re-declared ID: "+curtext);
			}
		}
		else if(store.peek().typename=="Deffunc"){
			//�������岿��,���ֺ���������ͺ�����������
			if(store.peek().operation=="Func") {		
				//���庯����������
				//��������Ĳ��������û��������������Ҫ�½�һ��table!
				if(!rootTable.map.containsKey(curtext)) {
					store.peek().funcname=curtext;  //������������Ϣ
					curTable.addEntry(curtext, curTable.curtype);
					FuncSymtable newtable=new FuncSymtable(curtext,true);
					curTable.addTable(newtable);
					curTable=newtable;
				}
				else {  //����ɸ������������ǲ����Ը��Ƕ���;
					int myindex=curTable.index.get(curtext).intValue();
					Symtable tmptable=curTable.children.get(myindex);
					if(tmptable instanceof FuncSymtable) {
						if( ((FuncSymtable) tmptable).isdefined) { 
							System.out.println("Error2!Redifined Function: "+curtext);
							curTable=tmptable;
						}
						else {
							curTable=tmptable;
							((FuncSymtable) curTable).isdefined=true;
						}
					}
					else {
						System.out.println("Expected Functable but not!");
					}
				}
				
			}
		}
		else if(store.peek().typename=="Paralist") {//ok
			//���������Ĳ�����ֻ��Ҫ��ע���ͣ���Ҫ��ע����
			if(store.peek().operation=="Declar") {
				curTable.addEntry(ctx.getText(), "null");
			}
			//��������ʱ�Ĳ�������Ҫע�����ֺ�����
			else if (store.peek().operation=="Deffunc") {
				curTable.addEntry(ctx.getText(), curTable.curtype);
			}
		}
		
	}
	@Override public void exitDirecid(@NotNull CParser.DirecidContext ctx) { }
	
	@Override public void enterParameterList(@NotNull CParser.ParameterListContext ctx) {
		MyNode father=store.peek();
		if(father.typename=="Deffunc") {
			MyNode tmpMyNode=new MyNode("Paralist");  
			tmpMyNode.operation="Deffunc";
			store.add(tmpMyNode);
		}
		else if(father.typename=="Declar"){
			MyNode tmpMyNode=new MyNode("Paralist");  
			tmpMyNode.operation="Declar";
			store.add(tmpMyNode);
		}
	}
	@Override public void exitParameterList(@NotNull CParser.ParameterListContext ctx) {
		store.pop();
		MyNode father=store.peek();
		//���������������������˳�������Ҫ����
		//System.out.println("back_to_parent");
		if(father.typename == "Declar")
			curTable=curTable.parent;
		//�����������˳�������ʱ�˳�
	}
	//���뺯��������
	@Override public void enterFunctionDefinition(@NotNull CParser.FunctionDefinitionContext ctx) {
		
		MyNode tmpMyNode=new MyNode("Deffunc");  
		tmpMyNode.operation="Deffunc";
		store.add(tmpMyNode);
	}

	@Override public void exitFunctionDefinition(@NotNull CParser.FunctionDefinitionContext ctx) {
		
		MyNode mynode=store.pop();
		MyNode father=store.peek();
		if(father.typename=="start") {
			father.addchild(mynode);
		}
	}
	
	@Override public void enterDirecFunc(@NotNull CParser.DirecFuncContext ctx) {
		//����ʱֻ�����ͼ�飡���������������Ҳ�����Ƕ���,��ʾ�������
		//���ͼ��������parameterList������δ�޸ģ����ű�Ҳû��ƺã�
		if(store.peek().typename=="Deffunc") {
			//�������壬����ΪDeffunc,Deffunc
			MyNode tmpMyNode=new MyNode("Deffunc");  
			tmpMyNode.operation="Func";
			store.add(tmpMyNode);
		}
		else if(store.peek().typename=="Declar") {
			//��������������ΪDeclar,Declar
			MyNode tmpMyNode=new MyNode("Declar");  
			tmpMyNode.operation="Func";
			store.add(tmpMyNode);
		}
		
	}
	
	@Override public void exitDirecFunc(@NotNull CParser.DirecFuncContext ctx) {
		MyNode tmpNode=store.pop();
		if(curTable.map.size()==0 && tmpNode.typename=="Declar") { //���ڽ���޲ε���������
			//System.out.println("No member!");
			curTable=curTable.parent;
		}
		store.peek().funcname=tmpNode.funcname;
	}

	@Override public void enterParameterDeclaration(@NotNull CParser.ParameterDeclarationContext ctx) {
		curTable.curtype=ctx.typespecifier().getText();
	}

	@Override public void exitParameterDeclaration(@NotNull CParser.ParameterDeclarationContext ctx) { }
	
	//����������������
	@Override public void enterDirecArr(@NotNull CParser.DirecArrContext ctx) {
		MyNode tmpMyNode=new MyNode("Declar");  
		//���������ά������ʼ���б���ʱû����ƺ�
		tmpMyNode.operation="Array";
		store.add(tmpMyNode);
	}
	@Override public void enterTypespecifier(@NotNull CParser.TypespecifierContext ctx) {
		curTable.curtype=ctx.getText();
	}

	@Override public void exitTypespecifier(@NotNull CParser.TypespecifierContext ctx) { }

	@Override public void enterCompoundStatement(@NotNull CParser.CompoundStatementContext ctx) {
		MyNode father=store.peek();
		//System.out.println("father is"+father.typename+"  "+father.operation);
		//Ŀǰ�������if�ƺ�Ҳ���ø�д
		if(father.typename!="Deffunc" && father.typename!="Loop") {
			Symtable newtable=new Symtable(String.valueOf(father.children.size()));
			//����һ�������ĺ���
			curTable.addTable(newtable);
			curTable=newtable;
		}
	}
	
	@Override public void exitCompoundStatement(@NotNull CParser.CompoundStatementContext ctx) {
		//if(store.peek().typename=="Deffunc") {
			curTable=curTable.parent;
		//}
	}
	@Override public void exitDirecArr(@NotNull CParser.DirecArrContext ctx) {
		store.pop();
	}
	
	//����Ϊ���㲿��
	@Override public void enterUnary(@NotNull CParser.UnaryContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp","unary");
		store.add(tmpMyNode);
	}

	@Override public void exitUnary(@NotNull CParser.UnaryContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}


	@Override public void enterUnaryOperator(@NotNull CParser.UnaryOperatorContext ctx) { 
		MyNode father=store.peek();
		if(father.typename=="Exp" && father.operation=="unary")
			father.addchild(new MyNode(ctx.getText(),ctx.getText()));
		else {
			System.out.println("Expected unary node but not!");
		}
	}
	
	@Override public void enterPostinc(@NotNull CParser.PostincContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp","Postinc");
		tmpMyNode.children.add(new MyNode(ctx.OP.getText(),ctx.OP.getText()));
		//System.out.println(tmpMyNode.typename+"in the stack!");ctxʶ�𵽵������ַ���
		store.add(tmpMyNode);
	}

	@Override public void exitPostinc(@NotNull CParser.PostincContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	
	@Override public void enterSuffinc(@NotNull CParser.SuffincContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp","Suffinc");
		tmpMyNode.children.add(new MyNode(ctx.OP.getText(),ctx.OP.getText()));
		//System.out.println(tmpMyNode.typename+"in the stack!");ctxʶ�𵽵������ַ���
		store.add(tmpMyNode);
	}

	@Override public void exitSuffinc(@NotNull CParser.SuffincContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	
	@Override public void enterMul(@NotNull CParser.MulContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp");
		tmpMyNode.operation=ctx.OP.getText();
		//System.out.println(tmpMyNode.typename+"in the stack!");ctxʶ�𵽵������ַ���
		store.add(tmpMyNode);
	}

	@Override public void exitMul(@NotNull CParser.MulContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	
	@Override public void enterAdd(@NotNull CParser.AddContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp");
		tmpMyNode.operation=ctx.OP.getText();
		//System.out.println(ctx.getText()); //ctxʶ�𵽵������ַ���
		store.add(tmpMyNode);
	}
	
	@Override public void exitAdd(@NotNull CParser.AddContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	@Override public void enterShift(@NotNull CParser.ShiftContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp");
		tmpMyNode.operation=ctx.OP.getText();
		store.add(tmpMyNode);
	}

	@Override public void exitShift(@NotNull CParser.ShiftContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}

	@Override public void enterRelop(@NotNull CParser.RelopContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp_relop");
		tmpMyNode.operation=ctx.OP.getText();
		store.add(tmpMyNode);
	}

	@Override public void exitRelop(@NotNull CParser.RelopContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	@Override public void enterEqu(@NotNull CParser.EquContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp_relop");
		tmpMyNode.operation=ctx.OP.getText();
		store.add(tmpMyNode);
	}

	@Override public void exitEqu(@NotNull CParser.EquContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	
	@Override public void enterAnd(@NotNull CParser.AndContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp_logic");
		tmpMyNode.operation="&&";
		store.add(tmpMyNode);
	}

	@Override public void exitAnd(@NotNull CParser.AndContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}

	@Override public void enterOr(@NotNull CParser.OrContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp_logic");
		tmpMyNode.operation="||";
		store.add(tmpMyNode);
	}

	@Override public void exitOr(@NotNull CParser.OrContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	
	@Override public void enterAssign(@NotNull CParser.AssignContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp_assign");
		tmpMyNode.operation=ctx.assignmentOperator().getText();
		//System.out.println(tmpMyNode.typename+"in the stack!");ctxʶ�𵽵������ַ���
		store.add(tmpMyNode);
	}

	@Override public void exitAssign(@NotNull CParser.AssignContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	@Override public void enterFuncall(@NotNull CParser.FuncallContext ctx) {
		MyNode tmpMyNode=new MyNode("Funcall");
		tmpMyNode.operation=ctx.primaryExpression().getText(); //������
		//System.out.println(tmpMyNode.typename+"in the stack!");ctxʶ�𵽵������ַ���
		store.add(tmpMyNode);
	}

	@Override public void exitFuncall(@NotNull CParser.FuncallContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	@Override public void enterArglist(@NotNull CParser.ArglistContext ctx) {
		MyNode tmpMyNode=new MyNode("Arglist","funcall");
		//System.out.println(tmpMyNode.typename+"in the stack!");ctxʶ�𵽵������ַ���
		store.add(tmpMyNode);
	}
	
	@Override public void exitArglist(@NotNull CParser.ArglistContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		MyNode.transfer(father, tmpMyNode);
	}
	
	@Override public void enterArray(@NotNull CParser.ArrayContext ctx) {
		MyNode tmpMyNode=new MyNode("Array");
		tmpMyNode.operation=ctx.primaryExpression().getText();
		//System.out.println(tmpMyNode.typename+"in the stack!");ctxʶ�𵽵������ַ���
		store.add(tmpMyNode);
	}

	@Override public void exitArray(@NotNull CParser.ArrayContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	@Override public void enterNewexp(@NotNull CParser.NewexpContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp","Primary");
		store.add(tmpMyNode);
	}
		//�������ţ�ʲô����������
	
	@Override public void exitNewexp(@NotNull CParser.NewexpContext ctx) {
		MyNode mynode=store.pop();
		MyNode father=store.peek();
		//�ƺ�����Ҫ��ע��ʲô��ֱ��ȫ���Ѷ��Ӹ�����
		MyNode.transfer(father, mynode);
	}
	@Override public void enterTo_const(@NotNull CParser.To_constContext ctx) {
		
		MyNode father=store.peek();
		if(father.typename!="Funcall" && father.typename!="Array") {
			MyNode last=new MyNode("const");  
			last.parent=father;
			last.operation=ctx.getText();
			father.children.add(last);
		}
	}

	@Override public void exitTo_const(@NotNull CParser.To_constContext ctx) { }
	@Override public void enterTo_id(@NotNull CParser.To_idContext ctx) {
		MyNode father=store.peek();
		String id=ctx.getText();
		if(father.typename!="Funcall" && father.typename!="Array") {
			MyNode last=new MyNode("identifier");  //ÿ���ҵ�identifier�ڵ㶼Ҫ��table��
			last.parent=father;
			last.operation=id;
			father.children.add(last);
		}
		if(Symtable.checkMissingId(curTable, id) ) {
			//trueΪmissing
			System.out.println("Error1:Identifier: "+id+" is missing");
		}
	}
	
}
