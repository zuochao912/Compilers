import java.util.ArrayList;

import javax.swing.event.InternalFrameAdapter;
import javax.swing.plaf.basic.BasicSplitPaneUI.KeyboardDownRightHandler;

import org.antlr.v4.parse.ANTLRParser.finallyClause_return;
import org.antlr.v4.parse.ANTLRParser.option_return;
import java.util.Stack;

//已完成，一元算术运算、函数调用的调用者部分;

public class IrGenerator {
	Symtable rootable;   //需要建立符号表
	static int count=0; //计数临时变量数目,特指寄存器变量而不是符号Label!
	static int Labelcount=0;  //计数生成的Label数目,生成对应的Label 为Label_k
	static final int  Mod=10; //避免寄存器数目太多
	Stack<String> Loopout;  //检查break、continue是否出错！
	Stack<String> Loopjudge;
	public IrGenerator(Symtable tmp) {
		rootable=tmp;
		Loopout=new Stack<String>();
		Loopjudge=new Stack<String>();
	}
	public void Generate(MyNode root) {  //是否需要符号表?还是初始化的时候一次搞定
		//Generate本身实际上只执行遍历工作，实际工作调用其它Generatecode实现
		String mytype=root.typename;
		String op=root.operation;
		if(mytype=="start") {
			for(int i=0;i<root.children.size();i++){
				Generate(root.children.get(i));
			}
		}
		else if(mytype=="Deffunc") {
			//System.out.println("In function "+root.funcname+" :");
			System.out.println("\n"+root.funcname+" :");
			for(int i=0;i<root.children.size();i++){
				Generate(root.children.get(i));
			}
		}
		else if(mytype=="Blocklist"){
			for(int i=0;i<root.children.size();i++){
				Generate(root.children.get(i));
			}
		}
		else if(mytype=="Exp_relop") {
			
			for(int i=root.children.size()-1;i>=0;i--) {
				MyNode tmpNode=root.children.get(i);
				if(tmpNode.typename!="identifier" && tmpNode.typename!="const" && tmpNode.typename!="string") { 
					//不是根节点
					Generate(tmpNode);
				}
			}
			MyNode left=root.children.get(0);
			MyNode right=root.children.get(1);
			String reg=Gencode2(op, left.operation, right.operation);
			root.operation=reg;
		}
		else if (mytype=="Exp_logic") {
			if(op=="&&") { //为什么这就认得出来，=就认不出来，++就认不出来？
				System.out.println("i am &&!");

				String trueDst="trudst";
				String falseDst="faldst";
				Generate_bool(root, trueDst, falseDst);
				
			}
			else if (op=="||") {
				System.out.println("i am ||!");

				String trueDst="trudst";
				String falseDst="faldst";
				Generate_bool(root, trueDst, falseDst);
				
			}
		}
		else if(mytype=="Array") {
			if(op=="index") { //翻译数组下标,还挺麻烦的，得*dim后再+index;(2*50+3)*10+5，是数组a[x][50][10]的a[2][3][5]元素
				int size=root.children.size();
				for(int i=0;i<size;i++) {
					MyNode tmpNode=root.children.get(i);
					String tmpreg=Gencode2("*", tmpNode.operation, String.valueOf(tmpNode.arrdim));
					tmpNode.typename=tmpreg;
				}
				if(size==1) {
					root.operation=root.children.get(0).typename;
				}
				else {
					for(int i=0;i<size-1;i++) {
						MyNode tmpNode1=root.children.get(i);
						MyNode tmpNode2=root.children.get(i+1);
						String tmpreg=Gencode2("*", tmpNode1.typename, tmpNode2.typename);
						tmpNode2.typename=tmpreg;
					}
					root.operation=root.children.get(size-1).typename;
				}
			}
			else { //取地址,这里要小心内存到内存，因此都把寻址结果放到寄存器中；用lea
				root.operation=root.children.get(0).operation+"["+root.children.get(1).operation+"]";
			}
		}
		else if (mytype=="Exp_assign") {
			//等号考虑到返回值和结合性
			MyNode left=root.children.get(0);
			MyNode right=root.children.get(1);
			if(right.typename!="identifier" || right.typename!="const") { 
				//不是根节点
				for(int i=root.children.size()-1;i>=0;i--) {
					Generate(root.children.get(i));
				}
			} 
			String tmpname=Gencodedst(root.operation , left.operation, right.operation);
			root.operation=tmpname;
		}
		else if (mytype=="Exp") { //关注一下一元变量和二元变量
			//后序遍历,且儿子从右向左遍历
			
			if(op=="unary") { //注意一下非运算在条件中的调用
				MyNode left=root.children.get(0);
				MyNode right=root.children.get(1); //把左儿子和右儿子抓起来生成代码
				String tmpname=Gencode1(left.operation, right.operation);
				root.operation=tmpname; 
			}
			else if(op=="Postinc") {
				//特殊处理,a++,返回的节点是reg=a的reg,再做a=a+1
				MyNode left=root.children.get(0);
				MyNode right=root.children.get(1);
				String Op= left.typename.substring(0, 1);
				String tmpname=Gencode1("=",right.operation);
				Gencodedst(Op, right.operation,"1",right.operation);  
				root.operation=tmpname;
			}
			else if(op=="Suffinc"){
				
				//特殊处理，a=a+1,返回节点为a的值
				MyNode left=root.children.get(0);
				MyNode right=root.children.get(1);
				String Op= left.typename.substring(0, 1);
				Gencodedst(Op, right.operation,"1", right.operation);
				root.operation=right.operation;
			}
			
			else if(op!="ifcondition"){ //正常的二元算术
				
				for(int i=root.children.size()-1;i>=0;i--) {
					MyNode tmpNode=root.children.get(i);
					if(tmpNode.typename!="identifier" && tmpNode.typename!="const" && tmpNode.typename!="string") { 
						//不是根节点
						Generate(tmpNode);
					}
				} 
					MyNode left=root.children.get(0);
					MyNode right=root.children.get(1);
					String tmpname=Gencode2(root.operation , right.operation,left.operation);
					//root.operation=left.operation;赋值的时候可以这样，但是已被单独提出
					root.operation=tmpname;
			}
			else { //遇到了ifcondition辅助节点
				
				Generate(root.children.get(0));
			}
		}
		else if(mytype=="Funcall") {  //函数调用生成代码
			//param x,k
			for(int i=root.children.size()-1;i>=0;i-- ) {
				MyNode tmp=root.children.get(i);
				if(tmp.typename!="identifier" && tmp.typename!="const" && tmp.typename!="string") { 
					//不是根节点
					Generate(tmp);
				}
				System.out.println("param  "+tmp.operation+"  "+i);
			}
			System.out.println("call  "+root.operation);
			String reg="Reg"+count;  //将返回结果放在一个变量里，此处就当作在寄存器中了
			count= (count == Mod-1) ? 0:count+1;
			System.out.println("retval  "+reg);
			root.operation=reg;
		}
		else if(mytype=="Selection") {//if -else三个儿子，
			//孩子一:condition，孩子2block，孩子3，else;
			String Lthen=newlabel("then");
			String Lelse=newlabel("else");
			String Lafter=newlabel("after");
			if(op=="if_single") {   //单独的if语句生成代码
				Generate_bool(root.children.get(0).children.get(0), Lthen, Lafter);
				System.out.println(Lthen+":");
				Generate(root.children.get(1));  //if-语句块
				System.out.println(Lafter);
			}
			else if(root.children.size()==3) { //if_else
				//System.out.println("In "+root.children.get(0).typename +"  and  " +root.children.get(0).operation);
				Generate_bool(root.children.get(0).children.get(0), Lthen, Lelse);
				System.out.println(Lthen+":   ");
				Generate(root.children.get(1));  //if-语句块
				System.out.println("goto  "+Lafter);
				System.out.println(Lelse);
				Generate(root.children.get(2)); //else-语句块
				System.out.println(Lafter);
			}
			else {//单独的else语句
				Generate(root.children.get(0));
			}
			
		}
		else if (mytype=="Loop") {  //对for循环生成代码
			String Ljudge=newlabel("loop_judge");
			String Lloop=newlabel("loop_block");
			String Lafter=newlabel("loop_after");
			Loopjudge.add(Ljudge);
			Loopout.add(Lafter);
			if(root.children.get(0).typename!="Loop")
				Generate(root.children.get(0));//exp1
			System.out.println("goto  "+Ljudge);
			System.out.println(Lloop+":");
			Generate(root.children.get(3));  //block
			Generate(root.children.get(2)); //exp3
			
			System.out.println(Ljudge+":");
			Generate_bool(root.children.get(1),Lloop , "null"); //exp2
			System.out.println(Lafter+":");
			Loopjudge.pop();
			Loopout.pop();
		}
		else if(mytype=="break") {
			if(Loopout.empty())
				System.out.println("break is not in the loop!");
			else 
				System.out.println("goto  "+Loopout.peek());
		}
		else if(mytype=="continue") {
			if(Loopjudge.empty())
				System.out.println("continue is not in the loop!");
			else 
				System.out.println("goto  "+Loopjudge.peek());
		}
		else if(mytype=="JumpLabel") {
			System.out.println(op+":");
		}
		else if(mytype=="Goto") {
			System.out.println("goto:  "+op);
		}
		else if(mytype=="Return") {
			if(root.children.size()!=0) {
				MyNode son=root.children.get(0);
				System.out.println("Retval  "+son.operation);
			}
			System.out.println("Ret");
			
		}
	}
	private static String newlabel(String Post) {
		String L1="Label_"+Labelcount+"_"+Post;
		Labelcount++;
		return L1;
	}
	private static String Gencode1(String Op,String src) {
		String reg="Reg"+count;  //注意如果是静态方法才能使用静态变量；不能出现动态变量
		count= (count == Mod-1) ? 0:count+1;
		System.out.println(Op+"  "+src+"  "+reg);
		return reg;
	}
	private static String Gencode2(String Op,String src1,String src2) {
		String reg="Reg"+count;  //注意如果是静态方法才能使用静态变量；不能出现动态变量
		count= (count == Mod-1) ? 0:count+1;
		System.out.println(Op+"  "+src1+"  "+src2+"  "+reg);
		return reg;
	}
	private static String Gencodedst(String Op,String src,String dst) {
		System.out.println(Op+"  "+src+"  "+dst);
		return dst; //似乎也只有等号是这样的了吧
	}
	private static String Gencodedst(String Op,String src1,String src2,String dst) {
		System.out.println(Op+"  "+src1+"  "+src2+"  "+dst);
		return dst; //似乎也只有等号是这样的了吧
	}
	public void Generate_bool(MyNode root,String truedst,String falsedst) {
		String mytype=root.typename;
		String op=root.operation;
		if(mytype=="Exp_relop") {
			if(root.children.size()==2) { //双目关系运算
				MyNode left=root.children.get(0);
				MyNode right=root.children.get(1);
				for(int i=root.children.size()-1;i>=0;i--) {
					MyNode tmpNode=root.children.get(i);
					if(tmpNode.typename!="identifier" && tmpNode.typename!="const" && tmpNode.typename!="string") { 
						//不是根节点
						Generate(tmpNode);
					}
				}
				String reg=Gencode2(op, left.operation, right.operation);
				root.operation=reg;
				System.out.println("JudgeTrue  "+reg+"  goto  "+truedst);
				if(falsedst!="null")
					System.out.println("goto  "+falsedst);
			}
			else { //可能直接只有一个数值!和最后那个else留谁呢？
				
			}
			
		}
		else if(op=="&&") { //如果成员是一个数而不是一个relop呢？
			//System.out.println("i am &&!");
			String L1="Label_"+Labelcount;
			Labelcount++;
			if(falsedst!="null")
				Generate_bool(root.children.get(0), L1, falsedst);
			System.out.print(L1+": ");
			if(falsedst!="null")
				Generate_bool(root.children.get(1), truedst, falsedst);
			
		}
		else if (op=="||") {
			String L1="Label_"+Labelcount;
			Labelcount++;
			
			Generate_bool(root.children.get(0), truedst, L1);
			System.out.print(L1+": ");
			if(falsedst!="null")
				Generate_bool(root.children.get(1), truedst, falsedst);
		}
		else if(op=="unary") {//单目运算非
			Generate(root);
			op=root.operation;
			System.out.println("JudgeTrue  "+op+"  goto  "+truedst);
			if(falsedst!="null")
				System.out.println("goto  "+falsedst);
		}
		else 
		{ //遇到一个数或者一个单目运算符的结果
			System.out.println("JudgeTrue  "+op+"  goto  "+truedst);
			if(falsedst!="null")
				System.out.println("goto  "+falsedst);
		}
	}
}
