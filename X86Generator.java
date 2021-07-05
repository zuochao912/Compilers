import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Stack;

import javax.naming.LinkLoopException;

public class X86Generator {
	static String [] relopStore= {"JL","JLE","JG","JGE","JE","JE"};
	static String [] alStore= {"ADD","SUB","IMUL","Imul","MOV"};
	static String [] regstore= {"EAX","EBX","ECX","EDX","EBP","ESP","ESI","EDI"};
	public Stack<String> Param;
	public void filereader(String file) throws IOException {
		Param=new Stack<String>();
		BufferedReader buf_reader = new BufferedReader(new FileReader(file));
        String str;
        while((str = buf_reader.readLine()) != null) {
            String line[]=str.split("  ");
            analyze(line);
            
        }  
	}
	public void analyze(String line[]) {
		int size=line.length; 
		int relop=0;
		
        if(line[0].equals("=")) {
        	//小心左右都是内存变量！无论如何，先放在EDX之中
        	System.out.println("MOV "+"EDX "+line[2]);
        	System.out.println("MOV "+line[1]+" EDX");
        }	
        else if(line[0].equals("&")) { //取地址运算，地址放在EBX
        	System.out.println("LEA EBX "+line[1]);
        	System.out.println("MOV "+line[2]+" EBX");
        }
         else if(line[0].equals("+")) {
        	//如果右值是内存变量，就把他挪到寄存器中！
        	System.out.println("MOV "+"EDX "+line[2]);  //总是放在EDX之中
        	System.out.println("ADD EDX "+line[1]);
        	if(size==4) {
        		System.out.println("MOV "+line[3]+" EDX");
        	}
        }
        else if(line[0].equals("-")) {
        	System.out.println("MOV "+"EDX "+line[2]);  //总是放在EDX之中
        	System.out.println("SUB EDX "+line[1]);
        	if(size==4) {
        		System.out.println("MOV "+line[3]+" EDX");
        	}
        }
        else if(line[0].equals("*")) {
        	//每次都把EDX清0！
        	System.out.println("MOV "+"EAX "+line[1]);
        	System.out.println("IMUL "+line[2]);
        	System.out.println("MOV "+line[3]+" EAX");
        }
        else if(line[0].equals("/")) {
        	//每次都把EDX扩展了！
        	System.out.println("MOV "+"EAX "+line[1]);
        	System.out.println("CDQ");
        	//左值挪到EAX里
        	System.out.println("IDIV "+line[2]);
        	//保留整除结果
        	System.out.println("MOV "+line[3]+" EAX");
	
        }
        else if(line[0].equals("%")) {
        	//每次都把EDX扩展了！
        	System.out.println("MOV "+"EAX "+line[1]);
        	System.out.println("CDQ");
        	//左值挪到EAX里
        	System.out.println("IDIV "+line[1]+" "+line[2]);
        	//保留余数
        	System.out.println("MOV "+line[3]+" EDX");
        }
        else if(line[0].equals("<")) {
        	relop=0;
        	System.out.println("MOV "+"EDX "+line[1]);
        	System.out.println("CMP EDX "+line[2]);
        }
        else if(line[0].equals("<=")) {
        	relop=1;
        	System.out.println("MOV "+"EDX "+line[1]);
        	System.out.println("CMP EDX "+line[2]);
        }
        else if(line[0].equals(">")) {
        	relop=2;
        	System.out.println("MOV "+"EDX "+line[1]);
        	System.out.println("CMP EDX "+line[2]);
        }
        else if(line[0].equals(">=")) {
        	relop=3;
        	System.out.println("MOV "+"EDX "+line[1]);
        	System.out.println("CMP EDX "+line[2]);
        }
        else if(line[0].equals("==")) {
        	relop=4;
        	System.out.println("MOV "+"EDX "+line[1]);
        	System.out.println("CMP EDX "+line[2]);
        }
        
        else if(line[0].equals("!=")) {
        	relop=5;
        	System.out.println("MOV "+"EDX "+line[1]);
        	System.out.println("CMP EDX "+line[2]);
        }
        else if(line[0].equals("JudgeTrue")) {
        	System.out.println(relopStore[relop]+" "+line[3]); //不看寄存器，也不用goto
        }
        else if(line[0].equals("goto")){
        	System.out.println("JMP "+line[1]);
        }
        else if(line[0].equals("param")) {
        	Param.add(line[1]);
        }
        else if(line[0].equals("call")) {
        	String ins="invoke "+line[1];
        	while(!Param.empty()) {
        		ins=ins+" "+Param.pop();
        	}
        	System.out.println(ins);
        }
        else if(line[0].equals("Retval")) {//返回值一律放回EAX
        	System.out.println("MOV "+"EAX "+line[1]);
        }
        else if(line[0].equals("retval")) { //取回返回值,为什么没有反应？
        	System.out.println("MOV "+line[1]+" EAX"); //从EAX放到草稿内存中
        }
        else {
        	//System.out.println("-----size:"+size);
        	for(int i=0;i<size;i++) {
        		System.out.print(line[i]+" ");
        	}
        	System.out.print("\n");
        }
	}
}
