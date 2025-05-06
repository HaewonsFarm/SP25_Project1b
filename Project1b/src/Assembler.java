import java.io.*;
import java.util.*;

/**
 * Assembler :
 * 이 프로그램은 SIC/XE 머신을 위한 Assembler 프로그램의 메인 루틴이다.
 * 프로그램의 수행 작업은 다음과 같다. <br>
 * 1) 처음 시작하면 Instruction 명세를 읽어들여서 assembler를 세팅한다. <br>
 * 2) 사용자가 작성한 input 파일을 읽어들인 후 저장한다. <br>
 * 3) input 파일의 문장들을 단어별로 분할하고 의미를 파악해서 정리한다. (pass1) <br>
 * 4) 분석된 내용을 바탕으로 컴퓨터가 사용할 수 있는 object code를 생성한다. (pass2) <br>
 *
 * <br><br>
 * 작성중의 유의사항 : <br>
 *  1) 새로운 클래스, 새로운 변수, 새로운 함수 선언은 얼마든지 허용됨. 단, 기존의 변수와 함수들을 삭제하거나 완전히 대체하는 것은 안된다.<br>
 *  2) 마찬가지로 작성된 코드를 삭제하지 않으면 필요에 따라 예외처리, 인터페이스 또는 상속 사용 또한 허용됨.<br>
 *  3) 모든 void 타입의 리턴값은 유저의 필요에 따라 다른 리턴 타입으로 변경 가능.<br>
 *  4) 파일, 또는 콘솔창에 한글을 출력시키지 말 것. (채점상의 이유. 주석에 포함된 한글은 상관 없음)<br>
 *
 * <br><br>
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class Assembler {
	/** instruction 명세를 저장한 공간 */
	private InstTable instTable;
	/** 읽어들인 input 파일의 내용을 한 줄 씩 저장하는 공간. */
	private ArrayList<String> lineList;
	/** 프로그램의 section별로 symbol table을 저장하는 공간*/
	private ArrayList<SymbolTable> symtabList;
	/** 프로그램의 section별로 프로그램을 저장하는 공간*/
	private ArrayList<TokenTable> tokenList;

	private ArrayList<LiteralTable> littabList;
	/**
	 * Token, 또는 지시어에 따라 만들어진 오브젝트 코드들을 출력 형태로 저장하는 공간. <br>
	 * 필요한 경우 String 대신 별도의 클래스를 선언하여 ArrayList를 교체해도 무방함.
	 */
	private ArrayList<String> codeList;

	// Pass1
	private int currentSection;
	private int locctr;
	private ArrayList<Integer> sectionStartAddr;

	/**
	 * 클래스 초기화. instruction Table을 초기화와 동시에 세팅한다.
	 *
	 * @param instFile : instruction 명세를 작성한 파일 이름.
	 */
	public Assembler(String instFile) {
		instTable = new InstTable("inst_table.txt");
		lineList = new ArrayList<>();
		symtabList = new ArrayList<>();
		tokenList = new ArrayList<>();
		littabList = new ArrayList<>();
		codeList = new ArrayList<>();
		sectionStartAddr = new ArrayList<>();
	}

	/**
	 * 어셈블러의 메인 루틴
	 */
	public static void main(String[] args) {
		Assembler asm = new Assembler("inst_table.txt");
		try {
			asm.loadInputFile("input.txt");
			asm.pass1();
			asm.printSymbolTable("output_symtab.txt");
			asm.printLiteralTable("output_littab.txt");
			asm.pass2();
			asm.printObjectCode("output_objectcode.txt");
		} catch (IOException e) {
			System.err.println("I/O Error: " + e.getMessage());
		}
	}


	/**
	 * inputFile을 읽어들여서 lineList에 저장한다.<br>
	 * @param inputFile : input 파일 이름.
	 */
	private void loadInputFile(String inputFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(inputFile));
		String line;
		while ((line = br.readLine()) != null) {
			line = line.trim();
			if (line.isEmpty() || line.startsWith(".")) continue;
			lineList.add(line);
		}
		br.close();
	}

	/**
	 * pass1 과정을 수행한다.<br>
	 *   1) 프로그램 소스를 스캔하여 토큰단위로 분리한 뒤 토큰테이블 생성<br>
	 *   2) label을 symbolTable에 정리<br>
	 *   <br><br>
	 *    주의사항 : SymbolTable과 TokenTable은 프로그램의 section별로 하나씩 선언되어야 한다.
	 */
	private void pass1() {
		// 초기화
		currentSection = -1;
		ArrayList<Token> deferredEquList = new ArrayList<>();

		// 각 라인 토큰화
		for (String line : lineList) {

			// 첫 섹션이 없으면 기본 섹션
			if (currentSection < 0) {
				symtabList.add(new SymbolTable());
				tokenList.add(new TokenTable(symtabList.get(0), instTable));
				littabList.add(new LiteralTable());
				currentSection = 0;
				locctr = 0;
				sectionStartAddr.add(locctr);
			}

			TokenTable tt = tokenList.get(currentSection);
			SymbolTable st = symtabList.get(currentSection);
			LiteralTable lt = littabList.get(currentSection);

			tt.putToken(line);
			Token tok = tt.getToken(tt.size() - 1);

			tok.location = locctr; // ⭐ 토큰에 위치 기록

			System.out.println("== DEBUG: Processing line ==");
			System.out.println(line);
			System.out.println("Token:");
			System.out.println("  Label    : " + tok.label);
			System.out.println("  Operator : " + tok.operator);
			System.out.println("  Operand  : " + Arrays.toString(tok.operand));
			System.out.println("  LOCCTR   : " + String.format("%04X", locctr));
			System.out.println("-------------------------------");

			// 새 섹션: START 또는 CSECT
			if ("START".equals(tok.operator)) {
				locctr = Integer.parseInt(tok.operand[0], 16);
				sectionStartAddr.set(currentSection, locctr);
				if (tok.label != null && !tok.label.isEmpty()) {
					if (st.searchSymbol(tok.label) == -1) {
						st.putSymbol(tok.label, locctr);
					}
				}
				continue;
			} else if ("CSECT".equals(tok.operator)) {
				currentSection++;
				symtabList.add(new SymbolTable());
				tokenList.add(new TokenTable(symtabList.get(currentSection), instTable));
				littabList.add(new LiteralTable());
				locctr = 0;
				sectionStartAddr.add(locctr);
				if (tok.label != null && !tok.label.isEmpty()) {
					symtabList.get(currentSection).putSymbol(tok.label, locctr);
				}
				continue;
			}

			// 리터럴 등록
			if (tok.operand != null && tok.operand.length > 0 && tok.operand[0].startsWith("=")) {
				lt.putLiteral(tok.operand[0]);
			}

			String mnemonic = tok.operator.startsWith("+") ? tok.operator.substring(1) : tok.operator;

			// 레이블 등록 (단, EQU는 나중에 처리하므로 제외!)
			if (!"EQU".equals(mnemonic)) {
				if (tok.label != null && !tok.label.isEmpty()) {
					st.putSymbol(tok.label, locctr);
				}
			}

			if ("EQU".equals(mnemonic)) {
				deferredEquList.add(tok);
				continue;
			}

			// LOCCTR 증가 처리
			if ("WORD".equals(mnemonic)) {
				locctr += 3;
			} else if ("RESW".equals(mnemonic)) {
				locctr += 3 * Integer.parseInt(tok.operand[0]);
			} else if ("RESB".equals(mnemonic)) {
				locctr += Integer.parseInt(tok.operand[0]);
			} else if ("BYTE".equals(mnemonic)) {
				String opnd = tok.operand[0];
				if (opnd.startsWith("C'") && opnd.endsWith("'")) {
					locctr += opnd.substring(2, opnd.length() - 1).length();
				} else if (opnd.startsWith("X'") && opnd.endsWith("'")) {
					String hex = opnd.substring(2, opnd.length() - 1);
					locctr += (hex.length() + 1) / 2;
				}
			} else if ("LTORG".equals(mnemonic)) {
				processLiteralPool(currentSection);
				continue;
			} else if ("END".equals(mnemonic)) {
				processLiteralPool(currentSection);
				break;
			} else {
				int fmt = instTable.getInstructionLength(tok.operator);
				if (fmt > 0) {
					locctr += fmt;
					System.out.println(">>Mnemonic: " + mnemonic + ", Format: " + fmt);
				}
			}
		}

		for (int sec = 0; sec < tokenList.size(); sec++) {
			SymbolTable st = symtabList.get(sec);
			for (Token eqTok : tokenList.get(sec).getTokenList()) {
				if ("EQU".equals(eqTok.operator)) {
					String label = eqTok.label;
					String expr = eqTok.operand[0];
					int value = 0;

					if (expr.equals("*")) {
						value = eqTok.location;
					} else if (expr.contains("-")) {
						String[] terms = expr.split("-");
						int a = st.getSymbol(terms[0].trim());
						int b = st.getSymbol(terms[1].trim());

						if (a == -1 || b == -1) {
							System.err.println("⚠ Undefined symbol in EQU: " + expr);
							continue;
						}
						value = a - b;
					} else {
						value = st.getSymbol(expr);
						if (value == -1) {
							System.err.println("⚠ Undefined symbol used in EQU: " + expr);
							continue;
						}
					}

					st.putSymbol(label, value);
				}
			}
		}
	}

	/**
	 * literal pool 처리: 아직 주소가 할당되지 않은(-1) 리터럴에 대해
	 * 현재 locctr 을 주소로 설정하고, 크기만큼 locctr 을 증가시킴
	 */
	/**
	 * literal pool 처리: 아직 주소가 -1인 리터럴에 대해
	 *   1) 현재 locctr을 주소로 설정
	 *   2) 리터럴 크기만큼 locctr을 증가
	 */
	private void processLiteralPool(int sec) {
		LiteralTable lt = littabList.get(sec);
		for (int i = 0; i < lt.size(); i++) {
			if (lt.getLocation(i) == -1) {
				lt.setLocation(i, locctr);

				String lit = lt.getLiteral(i);
				int size = 0;

				if (lit.startsWith("=C'") && lit.endsWith("'")) {
					size = lit.substring(3, lit.length() - 1).length();
				} else if (lit.startsWith("=X'") && lit.endsWith("'")) {
					int hexLen = lit.substring(3, lit.length() - 1).length();
					size = (hexLen + 1) / 2;
				}

				locctr += size;
			}
		}
	}

	private void printSymbolTable(String fileName) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		for (int i=0; i<symtabList.size(); i++){
			bw.write("[Section " + (i + 1) + "] Symbol Table"); bw.newLine();
			bw.write(symtabList.get(i).toString()); bw.newLine();
		}
		bw.close();
	}

	private void printLiteralTable(String fileName) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		for (int i = 0; i < littabList.size(); i++) {
			bw.write("[Section " + (i + 1) + "] Literal Table"); bw.newLine();
			bw.write(littabList.get(i).toString()); bw.newLine();
		}
		bw.close();
	}

	/**
	 * pass2 과정을 수행한다.<br>
	 *   1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
	 */
	private void pass2() {
		// TODO Auto-generated method stub

	}

	/**
	 * 작성된 codeList를 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printObjectCode(String fileName) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		for (String rec : codeList) {
			bw.write(rec);
			bw.newLine();
		}
		bw.close();
	}
}