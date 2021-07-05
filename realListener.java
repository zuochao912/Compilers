//基本完美
import java.nio.file.FileSystemLoopException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.sql.rowset.RowSetMetaDataImpl;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.semantics.SymbolChecks;
import org.omg.IOP.TaggedComponentHelper;

class Symtable{
	public Map<String, symNode> map;  //符号项目，符号名:类型名,其实这里应该改成“Node”节点才是合理的，但先解决别的问题
	public ArrayList<Symtable> children;  //儿子节点
	public Map <String, Integer > index; //用于找儿子表，映射函数名,符号名:在children的下标
	public Symtable parent;  //父亲表
	public String tablename;  //表名字，函数有名字；非函数,如语句块，for、if等都，无名字,用下标代替
	public String curtype;  //当前符号项目的类型
	static int string_count=0;
	//每次进入一个block模块时，在本节点建立一个新的block，并进入这个block；
	//当发现符号定义模块时，将符号添加到本block中;当遇到标识符时，从这个block开始寻找，一直找到全局block;
	//如果没有，那么没有定义；变量如果在本block找到定义，那么重复定义；如果是函数，那么不允许任何地方重复定义
	
	public Symtable(String name) {
		map= new HashMap<String,symNode>();
		index =new HashMap<String,Integer>();
		children=new ArrayList<Symtable>();
	
		tablename=name;
	}
	
	public void addEntry(String key,symNode val) {
		//key:参数名,val:参数类型
		if(!map.containsKey(key)) {
			//index.put(key, children.size());写错乱了。。
			map.put(key, val);
		}
		/*else  if(map.get(key)=="null") {
			map.put(key, val);
		}这是什么？*/
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
		Iterator<Map.Entry<String, symNode>> entries = now.map.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry<String, symNode> entry = entries.next();
			System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue().type);
			if(entry.getValue() instanceof arrNode) {
				arrNode newnode=(arrNode)entry.getValue();
				//System.out.println("In array!");
				for(int i=0;i<newnode.dim;i++) {
					System.out.println("dim"+i+"has the size of:"+newnode.dimList.get(i));
				}
			}
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


class symNode{
	//作为符号表中的变量
	public String type; //符号类型,由于不对函数作类型检查，所以不单独设立函数的了
	public symNode(String mytype) {
		type=mytype;
	}
}

class arrNode extends symNode{
	public int dim;
	public int totalsize;
	public ArrayList<Integer> dimList;
	public arrNode(String mytype) {
		super(mytype);
		// TODO Auto-generated constructor stub
		dimList=new ArrayList<Integer>();
	}
	public void setdim() {
		dim=dimList.size();
	}
	public void setdimval(int val) {
		dimList.add(val);
	}
	public void calsize() {
		totalsize=1;
		for(int i=0;i<dimList.size();i++) {
			totalsize*=dimList.get(i);
		}
	}
}

class funcNode extends symNode{
	public int argnum;//参数数量
	public String returntype;  //函数返回值类型
	public ArrayList<String> argtypeList; //参数名类型
	public funcNode(String mytype) {
		super(mytype);
		argnum=0;
		argtypeList=new ArrayList<String>();
		// TODO Auto-generated constructor stub
	}
	public void addarg(String name) {
		argtypeList.add(name);
		argnum++;
	}
	public void setReturnType(String type) {
		returntype=type;
	}
}

class MyNode {
    public String typename;  //原先的规则名称
    public String operation;  //作为ast节点的操作名,实际上ast的操作是operation啊！注意
	public MyNode parent;
	public ArrayList<MyNode> children ;
	public String funcname; //一开始设计的时候大意了，忘了IR要做函数标记了
	public int arrdim; //又大意了，数组生成IR需要维数信息
	public MyNode() {
		typename="";
		children = new ArrayList<MyNode>();
		parent=null;
		arrdim=1;
	}
	public MyNode(String name) {
		typename=name;
		children=new ArrayList<MyNode>();
		parent=null;
		arrdim=1;
	}
	public MyNode(String name, String op) {
		typename=name;
		children=new ArrayList<MyNode>();
		parent=null;
		operation=op;
		arrdim=1;
	}
	public void addchild(MyNode son) {
		son.parent=this;
		children.add(son);
	}
	static public void Tra(MyNode root,int depth) { //先序遍历
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
	Symtable rootTable;  //符号表根节点
	Symtable curTable;   //当前表
	boolean fordeclared;
	int arrDimCount;
	String arrname;
	//以下为顶层流程控制部分，包括顺序执行语句块、for循环、if-else、跳转表等等
	@Override public void enterCompilationUnit(@NotNull CParser.CompilationUnitContext ctx) {
		store=new Stack<MyNode>();
		rootnode=new MyNode("start");
		rootnode.operation="start";
		store.add(rootnode);
		
		rootTable=new Symtable("Root");
		rootTable.parent=null;
		curTable=rootTable;
		fordeclared=false;
		arrDimCount=0;
		arrname="null";
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
				System.out.println("Error!The node in the stack is "+result.typename+" op: "+result.operation);
			}
		}
		IrGenerator IR=new IrGenerator(rootTable);
		System.out.println("\n\nGenerating IR!");
		IR.Generate(result);
	}

	//我认为blockitem不用入栈，只需要把blockitemlist和statement入栈就行了
	@Override public void enterBlockItemList(@NotNull CParser.BlockItemListContext ctx) {
		MyNode tmpNode=new MyNode("Blocklist","Blocklist");
		store.add(tmpNode);
		
	}

	@Override public void exitBlockItemList(@NotNull CParser.BlockItemListContext ctx) { 
		MyNode result=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Deffunc" && father.operation=="Deffunc") {
			//函数定义，传递节点给父亲Deffunc;感觉是传递好
			MyNode.transfer(father, result);
			//father.addchild(result);
		}
		else if(father.typename=="Selection") {
			//作为if或者else的语句块，传递自己,感觉是挂载好
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
				father=store.peek(); //补上一个节点
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
		if(father.typename=="Selection" ) {//if_single和if_else
			//特别作为if的条件语句单列
			MyNode tmpNode=new MyNode("Exp","ifcondition"); //好像有
			store.add(tmpNode);
		}
		else { //普通的运算
			MyNode tmpNode=new MyNode("Exp","Exp");
			store.add(tmpNode);
		}
		
	}
	
	@Override public void exitExpression(@NotNull CParser.ExpressionContext ctx) {
	
		MyNode myNode=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Selection") {
			//将运算挂载到if节点,应该只能是作为if的条件
			father.addchild(myNode);
		}
		else if(father.typename=="Blocklist") {
			//传递给blocklist，我们不保留exp本身
			MyNode.transfer(father, myNode);
		}
		else if(father.typename=="Exp"&& father.operation=="Primary"){ 
			//出现了括号表达式！一般表达式传递给blockitemlist
			MyNode.transfer(father, myNode);
		}
		else if(father.typename=="Return" && father.operation=="return") {
			MyNode.transfer(father, myNode);
		}
		else if(father.typename=="Array"&& father.operation=="index") {
			MyNode.transfer(father, myNode);
		}
		//要区分exp作为while、for的条件和一般的语句
	}
//单个if的流程控制部分:
	@Override public void enterIf_single(@NotNull CParser.If_singleContext ctx) { 
		MyNode tmpNode=new MyNode("Selection","if_single");//if_else的开头
		store.add(tmpNode);
	}
	
	@Override public void exitIf_single(@NotNull CParser.If_singleContext ctx) {
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Selection") {  
			if(father.operation=="else") {//将if挂载到父亲else上,其直接父亲statement应该不起作用
				MyNode.transfer(father, tmpNode);
			}
		}
		else if(father.typename=="Blocklist") {
			//将本if挂载到blocklist之中
			father.addchild(tmpNode);
		}
	}
	
//if-else流程控制部分:
	@Override public void enterIf_else(@NotNull CParser.If_elseContext ctx) {
		MyNode tmpNode=new MyNode("Selection","if_else");//if_else的开头
		store.add(tmpNode);
	}

	@Override public void exitIf_else(@NotNull CParser.If_elseContext ctx) {
		//如果父亲节点是else，那么就转移，否则挂载；
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		if(father.typename=="Selection") {  
			if(father.operation=="else") {//将if挂载到父亲else上,其直接父亲statement应该不起作用
				MyNode.transfer(father, tmpNode);
			}
		}
		else if(father.typename=="Blocklist") {
			//将本if挂载到blocklist之中
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
			father.addchild(tmpNode); //把儿子转移给if语句
		}
		else {
			System.out.println("Expect selection but not!-->"+father.typename);
		}
	}
	
//for循环部分
	@Override public void enterForloop(@NotNull CParser.ForloopContext ctx) {
		MyNode tmpNode=new MyNode("Loop","forloop");//for循环的开头
		store.add(tmpNode);
		//注意，在for的block进入之前，需要观察for有几个孩子！写在blocklist处！
	}

	@Override public void exitForloop(@NotNull CParser.ForloopContext ctx) {
		//for循环直接挂载到父亲节点
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpNode);
		fordeclared=false;
	}
	@Override public void enterForDeclaration(@NotNull CParser.ForDeclarationContext ctx) {
		MyNode tmpNode=new MyNode("Loop","fordeclar");//for循环的开头
		store.add(tmpNode);
		Symtable tmptable=new Symtable("forloop");
		curTable.addTable(tmptable);
		curTable=tmptable;
		curTable.curtype=ctx.typespecifier().getText();
		System.out.println("new table in forloop!");
		fordeclared=true;
	}
	@Override public void exitForDeclaration(@NotNull CParser.ForDeclarationContext ctx) {
		//curTable=curTable.parent; 似乎compoundstate会代劳,因此不用
		MyNode tmpNode=new MyNode("Loop","forcondition");
		store.pop();
		MyNode father=store.peek();
		father.addchild(tmpNode);
	}
	@Override public void enterInitequ(@NotNull CParser.InitequContext ctx) { //初始化列表的定义变量
		MyNode father=store.peek();
		if(father.typename=="Loop" && father.operation=="fordeclar") {
			symNode newNode=new symNode(curTable.curtype);
			curTable.addEntry(ctx.directDeclarator().getText(),newNode );
		}
		MyNode son=new MyNode("Declar","="); //新加的
		store.push(son);
	}
	@Override public void exitInitequ(@NotNull CParser.InitequContext ctx) {//新加的
		MyNode son=store.pop();  
		MyNode father=store.peek();
		System.out.println("INITEQU:"+son.children.size());
		son.typename="Exp";
		father.addchild(son);
	}
	@Override public void enterInitdirec(@NotNull CParser.InitdirecContext ctx) {
		MyNode father=store.peek();
		if(father.typename=="Loop" && father.operation=="fordeclar") {
			symNode newNode=new symNode(curTable.curtype);
			curTable.addEntry(ctx.directDeclarator().getText(),newNode);  
		}
			
	}
	@Override public void enterForExpression(@NotNull CParser.ForExpressionContext ctx) {
		
		MyNode tmpNode=new MyNode("Loop","forconditon");//for循环的开头
		store.add(tmpNode);
	}
	
	@Override public void exitForExpression(@NotNull CParser.ForExpressionContext ctx) {
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		//非空则不用补上空;注意这里不是从exp上来的
		MyNode.transfer(father, tmpNode);	
	}
	//注意，这个forcondition需要推断自己是第一个还是第二个，如果前一个为空
	
//return部分:
	@Override public void enterReturn(@NotNull CParser.ReturnContext ctx) {
		MyNode tmpNode=new MyNode("Return","return");//if_else的开头
		store.add(tmpNode);
	}
	
	@Override public void exitReturn(@NotNull CParser.ReturnContext ctx) {
		MyNode tmpNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpNode);
	}
//goto部分	
	@Override public void enterLabel(@NotNull CParser.LabelContext ctx) {
		//定义标识符
		symNode newnode=new symNode("JumpLabel");
		curTable.addEntry(ctx.Identifier().getText(),newnode);
		MyNode tmpNode=new MyNode("JumpLabel",ctx.Identifier().getText());//否则IR生成找不到了
		MyNode father=store.peek();
		father.addchild(tmpNode);
	}
	@Override public void enterGoto(@NotNull CParser.GotoContext ctx) {
		//跳转到标识符
		MyNode tmpNode=new MyNode("Goto",ctx.Identifier().getText());
		MyNode father=store.peek();
		father.addchild(tmpNode);
	}
//break和continue;
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
	//以下为定义、声明部分
	@Override public void enterDeclaration(@NotNull CParser.DeclarationContext ctx) {
		//函数声明或者变量声明的语句
		MyNode tmpMyNode=new MyNode("Declar");  
		tmpMyNode.operation="Declar";
		store.add(tmpMyNode);
		curTable.curtype=ctx.typespecifier().getText(); //标明当前声明的类型
		//System.out.println(curTable.curtype);
	}
		

	@Override public void exitDeclaration(@NotNull CParser.DeclarationContext ctx) {
		MyNode son=store.pop();
		//由于存在初始化列表的情况，不能完全放在定义之中！以下为新加的
		MyNode father=store.peek();
		MyNode.transfer(father, son);
	}
	
	@Override public void enterDirecid(@NotNull CParser.DirecidContext ctx) { //特指directDeclarator ->id语句 
		//声明的标识符部分，包括一般变量，数组和函数调用
		//如果是函数定义，那么需要复写table!
		//如果是函数声明，那么就是第一次写，不过不允许重复声明！
		//变量声明，不允许重复声明
		String curtext=ctx.getText();
		//System.out.println(ctx.getText());
		MyNode father=store.peek();
		if(father.typename=="Declar") {
			//变量声明或者函数声明部分
			if(!Symtable.checkRedec(curTable,curtext)) {
				if(father.operation=="Func") { //声明函数的名称,
					symNode newnode=new funcNode("Function");
					curTable.addEntry(curtext, newnode);
					store.peek().funcname=curtext;
				}
				else if(father.operation=="Array") {  //声明一个数组ok
					symNode newnode=new arrNode("Array");
					curTable.addEntry(curtext, newnode);
					arrname=curtext;
				}
				else if(father.operation!="="){  //声明一个普通变量，应该也是ok的
					symNode newnode=new symNode(curTable.curtype);
					curTable.addEntry(curtext, newnode);
				}
				else { //新加的
					MyNode son=new MyNode("identifier",ctx.getText());  
					father.addchild(son);
				}
			}
			else {
				System.out.println("Error2!Re-declared ID: "+curtext);
			}
		}
		else if(store.peek().typename=="Deffunc"){
			//函数定义部分,区分函数参数表和函数本身名字
			if(store.peek().operation=="Func") {		
				//定义函数本身名字
				//函数定义的参数表，需要新建一个table!
				store.peek().funcname=curtext;  //保留函数名信息
				symNode newnode=new symNode("Function");
				curTable.addEntry(curtext, newnode);  //curTable.curtype
				Symtable newtable=new Symtable(curtext);
				curTable.addTable(newtable);
				curTable=newtable;
				System.out.println("new table in Deffunc!");
			}
		}
		else if(store.peek().typename=="Paralist") {//ok
			//函数声明的参数表，只需要关注类型，不要关注名字
			if(store.peek().operation=="Declar") {  //改成修改symnode!
				//curTable.addEntry(ctx.getText(), "null");
			}
			//函数定义时的参数表，需要注册名字和类型
			else if (store.peek().operation=="Deffunc") {
				symNode newnode=new symNode( curTable.curtype);
				curTable.addEntry(ctx.getText(),newnode);
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
		//函数声明，参数表查完就退出，都需要返回
		//System.out.println("back_to_parent");
		/*if(father.typename == "Declar")
			curTable=curTable.parent;*/
		//函数定义在退出函数体时退出
	}
	//进入函数定义大块
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
		//声明时只做类型检查！这里可能是声明，也可能是定义,提示后序操作
		//类型检查依赖于parameterList规则，尚未修改，符号表也没设计好！
		if(store.peek().typename=="Deffunc") {
			//函数定义，父亲为Deffunc,Deffunc
			MyNode tmpMyNode=new MyNode("Deffunc");  
			tmpMyNode.operation="Func";
			store.add(tmpMyNode);
		}
		else if(store.peek().typename=="Declar") {
			//函数声明，父亲为Declar,Declar
			MyNode tmpMyNode=new MyNode("Declar");  
			tmpMyNode.operation="Func";
			store.add(tmpMyNode);
		}
		
	}
	
	@Override public void exitDirecFunc(@NotNull CParser.DirecFuncContext ctx) {
		MyNode tmpNode=store.pop();
		/*if(curTable.map.size()==0 && tmpNode.typename=="Declar") { //用于解决无参的声明问题
			//System.out.println("No member!");
			curTable=curTable.parent;
		}*/
		store.peek().funcname=tmpNode.funcname; //这是什么来着？
	}

	@Override public void enterParameterDeclaration(@NotNull CParser.ParameterDeclarationContext ctx) {
		curTable.curtype=ctx.typespecifier().getText();
	}

	@Override public void exitParameterDeclaration(@NotNull CParser.ParameterDeclarationContext ctx) { }
	
	//进入数组声明部分
	@Override public void enterDirecArr(@NotNull CParser.DirecArrContext ctx) {
		MyNode tmpMyNode=new MyNode("Declar");  
		//后序的数组维数、初始化列表暂时没有设计好
		tmpMyNode.operation="Array";
		arrDimCount++;
		store.add(tmpMyNode);
	}
	@Override public void exitDirecArr(@NotNull CParser.DirecArrContext ctx) {
		store.pop();
		arrDimCount--;
		if(arrDimCount==0) {
			arrNode newnode=(arrNode)curTable.map.get(arrname);
			newnode.setdim();
			newnode.calsize();
			arrname="null";
		}
	}
	
	@Override public void enterTypespecifier(@NotNull CParser.TypespecifierContext ctx) {
		curTable.curtype=ctx.getText();
	}

	@Override public void exitTypespecifier(@NotNull CParser.TypespecifierContext ctx) { }

	@Override public void enterCompoundStatement(@NotNull CParser.CompoundStatementContext ctx) {
		MyNode father=store.peek();
		//System.out.println("father is"+father.typename+"  "+father.operation);
		//目前这里对于if似乎也不用改写
		if(father.typename!="Deffunc" || father.typename=="Loop"&& fordeclared==false) {
			Symtable newtable=new Symtable(String.valueOf(father.children.size()));
			//生成一个匿名的孩子
			curTable.addTable(newtable);
			curTable=newtable;
			System.out.println("new table in Compound!");
		}
	}
	
	@Override public void exitCompoundStatement(@NotNull CParser.CompoundStatementContext ctx) {
			curTable=curTable.parent;
			System.out.println("exit table in compound!");
		
	}

	
	//以下为运算部分
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
		//System.out.println(tmpMyNode.typename+"in the stack!");ctx识别到的整个字符串
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
		//System.out.println(tmpMyNode.typename+"in the stack!");ctx识别到的整个字符串
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
		//System.out.println(tmpMyNode.typename+"in the stack!");ctx识别到的整个字符串
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
		//System.out.println(ctx.getText()); //ctx识别到的整个字符串
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
		//System.out.println(tmpMyNode.typename+"in the stack!");ctx识别到的整个字符串
		store.add(tmpMyNode);
	}

	@Override public void exitAssign(@NotNull CParser.AssignContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	@Override public void enterFuncall(@NotNull CParser.FuncallContext ctx) {
		MyNode tmpMyNode=new MyNode("Funcall");
		tmpMyNode.operation="null";
		//tmpMyNode.operation=ctx.primaryExpression().getText(); //函数名
		//System.out.println(tmpMyNode.typename+"in the stack!");ctx识别到的整个字符串
		store.add(tmpMyNode);
	}

	@Override public void exitFuncall(@NotNull CParser.FuncallContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	@Override public void enterArglist(@NotNull CParser.ArglistContext ctx) {
		MyNode tmpMyNode=new MyNode("Arglist","funcall");
		//System.out.println(tmpMyNode.typename+"in the stack!");ctx识别到的整个字符串
		store.add(tmpMyNode);
	}
	
	@Override public void exitArglist(@NotNull CParser.ArglistContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		MyNode.transfer(father, tmpMyNode);
	}
	
	@Override public void enterArray(@NotNull CParser.ArrayContext ctx) {
		//运算时的数组
		MyNode tmpMyNode=new MyNode("Array");
		tmpMyNode.operation="Mem"; //数组其实就是赵memory
		MyNode tmpMyNode1=new MyNode("identifier",ctx.primaryExpression().getText());
		tmpMyNode.addchild(tmpMyNode1);
		//System.out.println(tmpMyNode.typename+"in the stack!");ctx识别到的整个字符串
		store.add(tmpMyNode);
	}

	@Override public void exitArray(@NotNull CParser.ArrayContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		father.addchild(tmpMyNode);
	}
	@Override public void enterArraygo(@NotNull CParser.ArraygoContext ctx) {
		//运算时对数组取下标运算
		MyNode tmpMyNode=new MyNode("Array","index");
		store.add(tmpMyNode);
	}

	@Override public void exitArraygo(@NotNull CParser.ArraygoContext ctx) {
		MyNode tmpMyNode=store.pop();
		MyNode father=store.peek();
		if(father.operation=="Mem") {
			father.addchild(tmpMyNode);
			int size=tmpMyNode.children.size();
			String name=father.children.get(0).operation;
			arrNode newnode=(arrNode)curTable.map.get(name);
			for(int i=0;i<size-1;i++) {
				tmpMyNode.children.get(i).arrdim=newnode.dimList.get(i+1);  //最后一个得是1，要补充一个！
			}
			tmpMyNode.children.get(size-1).arrdim=1;
		}
			
		else 	
			MyNode.transfer(father, tmpMyNode);
	}
	
	@Override public void enterNewexp(@NotNull CParser.NewexpContext ctx) {
		MyNode tmpMyNode=new MyNode("Exp","Primary");
		store.add(tmpMyNode);
	}
		//进入括号，什么都不用做！
	
	@Override public void exitNewexp(@NotNull CParser.NewexpContext ctx) {
		MyNode mynode=store.pop();
		MyNode father=store.peek();
		//似乎不需要关注是什么，直接全部把儿子给父亲
		MyNode.transfer(father, mynode);
	}
	@Override public void enterTo_const(@NotNull CParser.To_constContext ctx) {
		
		MyNode father=store.peek();
		System.out.println(father.typename+"  "+father.operation);
		if(father.typename!="Declar") { //原本第二个是&& father.typename!="Array"
			//原本条件是：father.typename!="Funcall" &&  father.typename!="Declar"
			MyNode last=new MyNode("const");  
			last.parent=father;
			last.operation=ctx.getText();
			father.children.add(last);
		}
		else if (father.typename=="Declar") {
			//数组的维数
			if(father.operation=="Array" && arrname!="null") {
				arrNode newnode=(arrNode)curTable.map.get(arrname);
				//System.out.println("Array dim!");
				newnode.setdimval(Integer.parseInt(ctx.getText()));
			}
			else if(father.operation=="=") {
				MyNode last=new MyNode("const",ctx.getText());  
				last.parent=father;
				father.children.add(last);
			}
		}
		
	}
	@Override public void exitTo_const(@NotNull CParser.To_constContext ctx) { }
	
	@Override public void enterTo_string(@NotNull CParser.To_stringContext ctx) {
		//注意string应该加入符号表！
		MyNode father=store.peek();
		if(father.typename!="Array") {
			//原本条件是：father.typename!="Funcall" &&  father.typename!="Declar"
			MyNode last=new MyNode("string");  
			last.parent=father;
			last.operation=ctx.getText();
			father.children.add(last);
			
			symNode newNode=new symNode(ctx.getText());
			rootTable.addEntry("string"+rootTable.string_count,newNode);
			rootTable.string_count++;
		}
	}

	@Override public void exitTo_string(@NotNull CParser.To_stringContext ctx) { }
	
	
	@Override public void enterTo_id(@NotNull CParser.To_idContext ctx) {
		MyNode father=store.peek();
		String id=ctx.getText();
		//System.out.println("ID:"+id);
		if(father.typename!="Array") {
			// father.typename!="Funcall" && 
			if(father.typename=="Funcall" && father.operation=="null")  //区分一下变量作为函数调用和函数参数；
				father.operation=id;
			else {
				MyNode last=new MyNode("identifier");  //每次找到identifier节点都要查table表！
				last.parent=father;
				last.operation=id;
				father.children.add(last);
			}
			
		}
		if(Symtable.checkMissingId(curTable, id) ) {
			//true为missing
			System.out.println("Error1:Identifier: "+id+" is missing");
		}
	}
	
}
