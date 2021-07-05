import java.util.ArrayList;

import javax.swing.event.InternalFrameAdapter;
import javax.swing.plaf.basic.BasicSplitPaneUI.KeyboardDownRightHandler;

import org.antlr.v4.parse.ANTLRParser.finallyClause_return;
import org.antlr.v4.parse.ANTLRParser.option_return;
import java.util.Stack;

//����ɣ�һԪ�������㡢�������õĵ����߲���;

public class IrGenerator {
	Symtable rootable;   //��Ҫ�������ű�
	static int count=0; //������ʱ������Ŀ,��ָ�Ĵ������������Ƿ���Label!
	static int Labelcount=0;  //�������ɵ�Label��Ŀ,���ɶ�Ӧ��Label ΪLabel_k
	static final int  Mod=10; //����Ĵ�����Ŀ̫��
	Stack<String> Loopout;  //���break��continue�Ƿ����
	Stack<String> Loopjudge;
	public IrGenerator(Symtable tmp) {
		rootable=tmp;
		Loopout=new Stack<String>();
		Loopjudge=new Stack<String>();
	}
	public void Generate(MyNode root) {  //�Ƿ���Ҫ���ű�?���ǳ�ʼ����ʱ��һ�θ㶨
		//Generate����ʵ����ִֻ�б���������ʵ�ʹ�����������Generatecodeʵ��
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
					//���Ǹ��ڵ�
					Generate(tmpNode);
				}
			}
			MyNode left=root.children.get(0);
			MyNode right=root.children.get(1);
			String reg=Gencode2(op, left.operation, right.operation);
			root.operation=reg;
		}
		else if (mytype=="Exp_logic") {
			if(op=="&&") { //Ϊʲô����ϵó�����=���ϲ�������++���ϲ�������
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
			if(op=="index") { //���������±�,��ͦ�鷳�ģ���*dim����+index;(2*50+3)*10+5��������a[x][50][10]��a[2][3][5]Ԫ��
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
			else { //ȡ��ַ,����ҪС���ڴ浽�ڴ棬��˶���Ѱַ����ŵ��Ĵ����У���lea
				root.operation=root.children.get(0).operation+"["+root.children.get(1).operation+"]";
			}
		}
		else if (mytype=="Exp_assign") {
			//�Ⱥſ��ǵ�����ֵ�ͽ����
			MyNode left=root.children.get(0);
			MyNode right=root.children.get(1);
			if(right.typename!="identifier" || right.typename!="const") { 
				//���Ǹ��ڵ�
				for(int i=root.children.size()-1;i>=0;i--) {
					Generate(root.children.get(i));
				}
			} 
			String tmpname=Gencodedst(root.operation , left.operation, right.operation);
			root.operation=tmpname;
		}
		else if (mytype=="Exp") { //��עһ��һԪ�����Ͷ�Ԫ����
			//�������,�Ҷ��Ӵ����������
			
			if(op=="unary") { //ע��һ�·������������еĵ���
				MyNode left=root.children.get(0);
				MyNode right=root.children.get(1); //������Ӻ��Ҷ���ץ�������ɴ���
				String tmpname=Gencode1(left.operation, right.operation);
				root.operation=tmpname; 
			}
			else if(op=="Postinc") {
				//���⴦��,a++,���صĽڵ���reg=a��reg,����a=a+1
				MyNode left=root.children.get(0);
				MyNode right=root.children.get(1);
				String Op= left.typename.substring(0, 1);
				String tmpname=Gencode1("=",right.operation);
				Gencodedst(Op, right.operation,"1",right.operation);  
				root.operation=tmpname;
			}
			else if(op=="Suffinc"){
				
				//���⴦��a=a+1,���ؽڵ�Ϊa��ֵ
				MyNode left=root.children.get(0);
				MyNode right=root.children.get(1);
				String Op= left.typename.substring(0, 1);
				Gencodedst(Op, right.operation,"1", right.operation);
				root.operation=right.operation;
			}
			
			else if(op!="ifcondition"){ //�����Ķ�Ԫ����
				
				for(int i=root.children.size()-1;i>=0;i--) {
					MyNode tmpNode=root.children.get(i);
					if(tmpNode.typename!="identifier" && tmpNode.typename!="const" && tmpNode.typename!="string") { 
						//���Ǹ��ڵ�
						Generate(tmpNode);
					}
				} 
					MyNode left=root.children.get(0);
					MyNode right=root.children.get(1);
					String tmpname=Gencode2(root.operation , right.operation,left.operation);
					//root.operation=left.operation;��ֵ��ʱ����������������ѱ��������
					root.operation=tmpname;
			}
			else { //������ifcondition�����ڵ�
				
				Generate(root.children.get(0));
			}
		}
		else if(mytype=="Funcall") {  //�����������ɴ���
			//param x,k
			for(int i=root.children.size()-1;i>=0;i-- ) {
				MyNode tmp=root.children.get(i);
				if(tmp.typename!="identifier" && tmp.typename!="const" && tmp.typename!="string") { 
					//���Ǹ��ڵ�
					Generate(tmp);
				}
				System.out.println("param  "+tmp.operation+"  "+i);
			}
			System.out.println("call  "+root.operation);
			String reg="Reg"+count;  //�����ؽ������һ��������˴��͵����ڼĴ�������
			count= (count == Mod-1) ? 0:count+1;
			System.out.println("retval  "+reg);
			root.operation=reg;
		}
		else if(mytype=="Selection") {//if -else�������ӣ�
			//����һ:condition������2block������3��else;
			String Lthen=newlabel("then");
			String Lelse=newlabel("else");
			String Lafter=newlabel("after");
			if(op=="if_single") {   //������if������ɴ���
				Generate_bool(root.children.get(0).children.get(0), Lthen, Lafter);
				System.out.println(Lthen+":");
				Generate(root.children.get(1));  //if-����
				System.out.println(Lafter);
			}
			else if(root.children.size()==3) { //if_else
				//System.out.println("In "+root.children.get(0).typename +"  and  " +root.children.get(0).operation);
				Generate_bool(root.children.get(0).children.get(0), Lthen, Lelse);
				System.out.println(Lthen+":   ");
				Generate(root.children.get(1));  //if-����
				System.out.println("goto  "+Lafter);
				System.out.println(Lelse);
				Generate(root.children.get(2)); //else-����
				System.out.println(Lafter);
			}
			else {//������else���
				Generate(root.children.get(0));
			}
			
		}
		else if (mytype=="Loop") {  //��forѭ�����ɴ���
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
		String reg="Reg"+count;  //ע������Ǿ�̬��������ʹ�þ�̬���������ܳ��ֶ�̬����
		count= (count == Mod-1) ? 0:count+1;
		System.out.println(Op+"  "+src+"  "+reg);
		return reg;
	}
	private static String Gencode2(String Op,String src1,String src2) {
		String reg="Reg"+count;  //ע������Ǿ�̬��������ʹ�þ�̬���������ܳ��ֶ�̬����
		count= (count == Mod-1) ? 0:count+1;
		System.out.println(Op+"  "+src1+"  "+src2+"  "+reg);
		return reg;
	}
	private static String Gencodedst(String Op,String src,String dst) {
		System.out.println(Op+"  "+src+"  "+dst);
		return dst; //�ƺ�Ҳֻ�еȺ����������˰�
	}
	private static String Gencodedst(String Op,String src1,String src2,String dst) {
		System.out.println(Op+"  "+src1+"  "+src2+"  "+dst);
		return dst; //�ƺ�Ҳֻ�еȺ����������˰�
	}
	public void Generate_bool(MyNode root,String truedst,String falsedst) {
		String mytype=root.typename;
		String op=root.operation;
		if(mytype=="Exp_relop") {
			if(root.children.size()==2) { //˫Ŀ��ϵ����
				MyNode left=root.children.get(0);
				MyNode right=root.children.get(1);
				for(int i=root.children.size()-1;i>=0;i--) {
					MyNode tmpNode=root.children.get(i);
					if(tmpNode.typename!="identifier" && tmpNode.typename!="const" && tmpNode.typename!="string") { 
						//���Ǹ��ڵ�
						Generate(tmpNode);
					}
				}
				String reg=Gencode2(op, left.operation, right.operation);
				root.operation=reg;
				System.out.println("JudgeTrue  "+reg+"  goto  "+truedst);
				if(falsedst!="null")
					System.out.println("goto  "+falsedst);
			}
			else { //����ֱ��ֻ��һ����ֵ!������Ǹ�else��˭�أ�
				
			}
			
		}
		else if(op=="&&") { //�����Ա��һ����������һ��relop�أ�
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
		else if(op=="unary") {//��Ŀ�����
			Generate(root);
			op=root.operation;
			System.out.println("JudgeTrue  "+op+"  goto  "+truedst);
			if(falsedst!="null")
				System.out.println("goto  "+falsedst);
		}
		else 
		{ //����һ��������һ����Ŀ������Ľ��
			System.out.println("JudgeTrue  "+op+"  goto  "+truedst);
			if(falsedst!="null")
				System.out.println("goto  "+falsedst);
		}
	}
}
