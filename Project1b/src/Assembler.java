import java.io.*;
import java.nio.charset.StandardCharsets;
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

 * <br><br>
 *  + 제공하는 프로그램 구조의 개선방법을 제안하고 싶은 분들은 보고서의 결론 뒷부분에 첨부 바랍니다. 내용에 따라 가산점이 있을 수 있습니다.
 */
public class Assembler {
	private static final int MAX_TEXT_RECORD_LENGTH = 30;
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

	private ArrayList<String> sectionNames;
	private ArrayList<Integer> sectionLengths;

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
		sectionNames = new ArrayList<>();
		sectionLengths = new ArrayList<>();
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
		boolean inWRREC = false;    // WRREC 디버깅용

		// 각 라인 토큰화
		for (String line : lineList) {
			// 첫 섹션이 없으면 기본 섹션 설정
			if (currentSection < 0) {
				symtabList.add(new SymbolTable());
				tokenList.add(new TokenTable(symtabList.get(0), instTable));
				littabList.add(new LiteralTable());
				currentSection = 0;
				locctr = 0;
				sectionStartAddr.add(locctr);
				sectionNames.add("NONAME");
				sectionLengths.add(0);
			}

			TokenTable tt = tokenList.get(currentSection);
			SymbolTable st = symtabList.get(currentSection);
			LiteralTable lt = littabList.get(currentSection);

			tt.putToken(line);
			Token tok = tt.getToken(tt.size() - 1);
			tok.location = locctr; // 토큰 위치 기록

			// START
			if ("START".equals(tok.operator)) {
				String secName = tok.label.isEmpty() ? "NONAME" : tok.label;
				// 첫 번째 섹션 이름을 변경
				sectionNames.set(0, secName);
				// locctr을 피연산자로 설정
				locctr = (tok.operand.length > 0 && !tok.operand[0].isEmpty())
						? Integer.parseInt(tok.operand[0], 16)
						: 0;
				// 시작 주소와 길이 초기화
				sectionStartAddr.set(0, locctr);
				sectionLengths.set(0, 0);
				// 레이블이 있으면 심볼 테이블에 등록
				if (!tok.label.isEmpty()) {
					st.putSymbol(tok.label, locctr);
				}
				continue;
			}

			// 새 섹션 시작: CSECT
			else if ("CSECT".equals(tok.operator)) {
				// WRREC 디버깅용
				String name = tok.label != null && !tok.label.isEmpty() ? tok.label : "NONAME";
				inWRREC = name.equals("WRREC");

				// 이전 섹션 리터럴 flush 및 길이 저장
				int prevLen = locctr - sectionStartAddr.get(currentSection);
				sectionLengths.set(currentSection, prevLen);

				// 새 섹션 설정
				currentSection++;
				String secName = tok.label != null && !tok.label.isEmpty() ? tok.label : "NONAME";
				sectionNames.add(secName);
				sectionLengths.add(0);
				sectionStartAddr.add(0);

				symtabList.add(new SymbolTable());
				tokenList.add(new TokenTable(symtabList.get(currentSection), instTable));
				littabList.add(new LiteralTable());
				locctr = 0;
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

			// 레이블 등록 (EQU 제외)
			if (!"EQU".equals(mnemonic) && tok.label != null && !tok.label.isEmpty()) {
				st.putSymbol(tok.label, locctr);
			}

			// EQU 처리 분리
			if ("EQU".equals(mnemonic)) {
				deferredEquList.add(tok);
				continue;
			}

			// LOCCTR 증가
			switch (mnemonic) {
				case "WORD":
					locctr += 3;
					break;
				case "RESW":
					locctr += 3 * Integer.parseInt(tok.operand[0]);
					break;
				case "RESB":
					locctr += Integer.parseInt(tok.operand[0]);
					break;
				case "BYTE":
					String opnd = tok.operand[0];
					if (opnd.startsWith("C'") && opnd.endsWith("'")) {
						locctr += opnd.substring(2, opnd.length() - 1).length();
					} else if (opnd.startsWith("X'") && opnd.endsWith("'")) {
						String hex = opnd.substring(2, opnd.length() - 1);
						locctr += (hex.length() + 1) / 2;
					}
					break;
				case "LTORG":
					processLiteralPool(currentSection);
					continue;
				case "END":
					processLiteralPool(currentSection);
					// 마지막 섹션 리터럴 flush 및 길이 저장
					int finalLen = locctr - sectionStartAddr.get(currentSection);
					sectionLengths.set(currentSection, finalLen);
					break;
				default:
					int fmt = instTable.getInstructionLength(tok.operator);
					if (fmt > 0) {
						locctr += fmt;
					}
			}

			if ("END".equals(mnemonic)) {
				break;
			}
		}

		// EQU 후처리
		for (int sec = 0; sec < tokenList.size(); sec++) {
			SymbolTable st = symtabList.get(sec);
			for (Token eqTok : tokenList.get(sec).getTokenList()) {
				if (!"EQU".equals(eqTok.operator)) continue;

				String label = eqTok.label;
				String expr = eqTok.operand[0].trim();
				int value;
				if ("*".equals(expr)) {
					value = eqTok.location;
				} else if (expr.contains("-")) {
					String[] terms = expr.split("-");
					if (terms.length == 2) {
						int a = st.getSymbol(terms[0].trim());
						int b = st.getSymbol(terms[1].trim());
						value = a - b;
					} else {
						continue;
					}
				} else {
					try {
						value = Integer.parseInt(expr);
					} catch (NumberFormatException e) {
						value = st.getSymbol(expr);
					}
				}
				st.putSymbol(label, value);
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
			bw.write(symtabList.get(i).toString()); bw.newLine();
		}
		bw.close();
	}

	private void printLiteralTable(String fileName) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		for (int i = 0; i < littabList.size(); i++) {
			bw.write(littabList.get(i).toString());
		}
		bw.close();
	}

	private String generateObjectCode(Token t, int currentOffset, SymbolTable st, LiteralTable lt, int baseReg) {
		String mnemonic = t.operator.startsWith("+") ? t.operator.substring(1) : t.operator;

		// WORD 상수
		if ("WORD".equalsIgnoreCase(mnemonic)) {
			String op = t.operand[0].trim();
			int value;
			if (op.contains("-")) {
				String[] parts = op.split("-");
				int a = st.getSymbol(parts[0].trim());
				int b = st.getSymbol(parts[1].trim());
				value = a - b;
			} else {
				value = Integer.parseInt(op);
			}
			// 3바이트(6hex)로 반환
			return String.format("%06X", value & 0xFFFFFF);
		}

		// BYTE 상수
		if ("BYTE".equalsIgnoreCase(mnemonic)) {
			String op = t.operand[0];
			byte[] data;
			if (op.startsWith("C'") && op.endsWith("'")) {
				data = op.substring(2, op.length()-1)
						.getBytes(StandardCharsets.US_ASCII);
			} else if (op.startsWith("X'") && op.endsWith("'")) {
				String hex = op.substring(2, op.length()-1);
				data = new byte[(hex.length()+1)/2];
				for (int i = 0; i < data.length; i++) {
					int start = 2*i;
					int end   = Math.min(start+2, hex.length());
					data[i]  = (byte)Integer.parseInt(hex.substring(start,end),16);
				}
			} else {
				// =12 같은 10진수 리터럴은 3바이트 WORD 크기
				int val = Integer.parseInt(op);
				data = new byte[] {
						(byte)((val >> 16)&0xFF),
						(byte)((val >>  8)&0xFF),
						(byte)( val        &0xFF)
				};
			}
			// 바이트 → HEX 문자열
			StringBuilder sb = new StringBuilder();
			for (byte b : data) sb.append(String.format("%02X", b & 0xFF));
			return sb.toString();
		}

		// format 1/2/3/4
		Instruction inst = instTable.getInst(mnemonic);
		if (inst == null) return "";

		int format = inst.format;
		if (t.operator.startsWith("+")) format = 4;

		// Format 1
		if (format == 1) {
			return String.format("%02X", inst.opcode);
		}

		// Format 2
		if (format == 2) {
			int r1 = getRegisterNumber(t.operand[0]);
			int r2 = (t.operand.length > 1 && !t.operand[1].isEmpty())
					? getRegisterNumber(t.operand[1])
					: 0;
			return String.format("%02X%X%X", inst.opcode, r1, r2);
		}

		// Format 3/4
		int opcode = inst.opcode & 0xFC;
		int n = 1, i = 1, x = 0, b = 0, p = 0, e = format == 4 ? 1 : 0;
		int targetAddr = 0;
		String op = t.operand[0];

		// RSUB은 operand 0으로 비워둠.
		if (format == 3 && "RSUB".equalsIgnoreCase(mnemonic)) {
			int opcode3 = (inst.opcode & 0xFC) | 0b11;
			return String.format("%02X%01X%03X", opcode3, 0, 0);
		}

		// 리터럴 처리
		if (op.startsWith("=")) {
			int litIndex = -1;
			for (int idx = 0; idx < lt.size(); idx++) {
				if (lt.getLiteral(idx).equals(op)) {
					litIndex = idx;
					break;
				}
			}
			if (litIndex >= 0) {
				targetAddr = lt.getLocation(litIndex);
			}
			if (format == 4) {
				e = 1;
			} else {
				e = 0;
			}
			// 리터럴은 항상 4형식 직접 주소 방식
			n = 1; i = 1;
		}
		// immediate (#) or indirect (@)
		else if (op.startsWith("#")) {
			n = 0; i = 1;
			String val = op.substring(1);
			try {
				// 즉시 상수만 special case
				int constVal = Integer.parseInt(val);
				b = 0; p = 0; x = 0; e = 0;
				// opcode 상위 비트, n/i 비트
				int op6 = (inst.opcode & 0xFC) | (n<<1) | i;
				int nixbpe = 0;
				// 즉시 상수값 채워서 리턴
				return String.format("%02X%01X%03X", op6, nixbpe, constVal & 0xFFF);
			} catch (NumberFormatException ex) {
				targetAddr = st.searchSymbol(val);
				if (targetAddr < 0) targetAddr = 0;
			}
		} else if (op.startsWith("@")) {
			n = 1; i = 0;
			targetAddr = st.searchSymbol(op.substring(1));
			if (targetAddr < 0) targetAddr = 0;
		} else {
			// Simple/direct
			targetAddr = st.searchSymbol(op);
			if (targetAddr < 0) targetAddr = 0;
		}

		// Indexed addressing
		if (t.operand.length > 1 && "X".equals(t.operand[1])) x = 1;

		int[] bpFlags = new int[2];
		int disp = calcDisp(targetAddr, currentOffset, baseReg, format, e == 1, bpFlags);
		b = bpFlags[0];
		p = bpFlags[1];

		int flags = (n << 1) | i;
		opcode |= flags;
		int nixbpe = (x << 3) | (b << 2) | (p << 1) | e;

		if (format == 3) {
			return String.format("%02X%01X%03X", opcode, nixbpe, disp & 0xFFF);
		} else {
			return String.format("%02X%01X%05X", opcode, nixbpe, targetAddr);
		}
	}

	private int getRegisterNumber(String reg) {
		return switch (reg.toUpperCase()) {
			case "A" -> 0;
			case "X" -> 1;
			case "L" -> 2;
			case "B" -> 3;
			case "S" -> 4;
			case "T" -> 5;
			case "F" -> 6;
			default -> 0;
		};
	}

	private List<String> generateModificationRecords(Token t, int currentOffset, Set<String> extDefSymbols, Set<String> extRefSymbols) {
		List<String> mods = new ArrayList<>();

		String op = t.operand[0];

		// 리터럴 M 레코드 (extended)
		if (op.startsWith("=")) {
			if (extRefSymbols.contains(op)) {
				mods.add(String.format("M%06X05+%s", currentOffset, op));
			}
		}

		// Format 4 명령어 (+)
		else if (t.operator.startsWith("+")) {
			String symbol = op.replaceAll("^[#@]", "").split(",")[0];
			if (extRefSymbols.contains(symbol)) {
				mods.add(String.format("M%06X05+%s", currentOffset + 1, symbol));
			}
		}

		// WORD 상대식
		if ("WORD".equalsIgnoreCase(t.operator)) {
			int modAddr = currentOffset;
			// 부호/심볼 파싱
			char sign = '+';
			int pos = 0, len = op.length();
			while (pos < len) {
				char c = op.charAt(pos);
				// 부호 만나면 저장
				if (c == '+' || c == '-') {
					sign = c;
					pos++;
					continue;
				}
				// 심볼 이름 읽기
				int start = pos;
				while (pos < len
						&& (Character.isLetterOrDigit(op.charAt(pos))
						|| op.charAt(pos) == '_')) {
					pos++;
				}
				if (start == pos) break;
				String sym = op.substring(start, pos);

				// EXTDEF 혹은 EXTREF 심볼이면 M 레코드 생성
				if (extRefSymbols.contains(sym)
					|| extDefSymbols.contains(sym)) {
					mods.add(String.format("M%06X06%c%s", modAddr, sign, sym));
				}
				// 다음 term은 기본 '+'
				sign = '+';
			}
		}

		// EQU 상대식
		if ("EQU".equalsIgnoreCase(t.operator) && op.contains("-")) {
			int modAddr = currentOffset;
			char sign = '+';
			int pos = 0, len = op.length();
			while (pos < len) {
				char c = op.charAt(pos);
				if (c == '+' || c == '-') {
					sign = c;
					pos++;
					continue;
				}
				int start = pos;
				while (pos < len &&
						(Character.isLetterOrDigit(op.charAt(pos)) || op.charAt(pos) == '_')) {
					pos++;
				}
				if (start == pos) break;
				String sym = op.substring(start, pos);

				if (extDefSymbols.contains(sym) || extRefSymbols.contains(sym)) {
					mods.add(String.format("M%06X06%c%s", modAddr, sign, sym));
				}
				sign = '+';
			}
		}

		return mods;
	}

	/**
	 * 해당 토큰이 T 레코드에 포함 가능한지 여부를 반환
	 * - START, END, CSECT, EXTDEF, EXTREF, EQU, RESW, RESB, LTORG 제외
	 * - 주석 또는 연산자 없는 경우 제외
	 */
	private boolean isTextRecordable(Token t) {
		if (t.comment != null && t.comment.startsWith(".")) return false;
		if (t.operator == null || t.operator.isEmpty()) return false;

		String op = t.operator.toUpperCase();

		return !(op.equals("START") || op.equals("END") || op.equals("CSECT") ||
				op.equals("EXTDEF") || op.equals("EXTREF") || op.equals("EQU") ||
				op.equals("RESW") || op.equals("RESB") || op.equals("LTORG"));
	}

	private int calcDisp(int target, int current, int base, int format, boolean isExtended, int[] bpFlags) {

		// format 4
		if (isExtended) {
			bpFlags[0] = 0;
			bpFlags[1] = 0;
			return target;
		}
		// format 3(pc)
		int instrLen = (format == 3 ? 3 : format);
		int pc = current + instrLen;
		int disp = target - pc;
		if (disp >= -2048 && disp <= 2047) {
			bpFlags[0] = 0;     // B = 0
			bpFlags[1] = 1;     // P = 1
			return disp & 0xFFF;
		}
		// format 3(base)
		disp = target - base;
		if (disp >= 0 && disp <= 0xFFF) {
			bpFlags[0] = 1;     // B = 1
			bpFlags[1] = 0;     // P = 0
			return disp & 0xFFF;
		}

		bpFlags[0] = 0;
		bpFlags[1] = 0;
		return 0;
	}

	private String getProgramName(TokenTable tt) {
		for (Token t : tt.getTokenList()) {
			if ((t.operator.equalsIgnoreCase("START") || t.operator.equalsIgnoreCase("CSECT"))
					&& t.label != null && !t.label.isEmpty()) {
				return String.format("%-6s", t.label);
			}
		}
		return "NONAME"; // fallback
	}

	/**
	 * pass2 과정을 수행한다.<br>
	 *   1) 분석된 내용을 바탕으로 object code를 생성하여 codeList에 저장.
	 */
	private void pass2() {
		// 각 컨트롤 섹션별로 H/D/R/T/M/E 레코드 생성
		for (int sec = 0; sec < tokenList.size(); sec++) {
			TokenTable tt = tokenList.get(sec);
			SymbolTable st = symtabList.get(sec);
			LiteralTable lt = littabList.get(sec);
			int secStart = sectionStartAddr.get(sec);
			int secLength = sectionLengths.get(sec);

			// 섹션별로 아직 처리되지 않은 리터럴 인덱스 모아두기
			List<Integer> litIdxs = new ArrayList<>();
			for (int i = 0; i < lt.size(); i++) {
				int loc = lt.getLocation(i);
				if (loc > secStart && loc < secStart + secLength) {
					litIdxs.add(i);
				}
			}

			// BASE 디렉티브로 설정된 base 레지스터 값 결정
			int baseReg = -1;
			for (Token t : tt.getTokenList()) {
				if ("BASE".equalsIgnoreCase(t.operator) && t.operand.length > 0) {
					baseReg = st.getSymbol(t.operand[0]);
					break;
				}
			}

			// 섹션 이름
			Token first = tt.getToken(0);
			String progName = (first.label != null && !first.label.isEmpty()) ? first.label : sectionNames.get(sec);

			// 출력할 내용이 없으면 건너뛰기
			boolean hasContent = false;
			for (Token t : tt.getTokenList()) {
				if ("EXTDEF".equalsIgnoreCase(t.operator)
						|| "EXTREF".equalsIgnoreCase(t.operator)
						|| isTextRecordable(t)) {
					hasContent = true;
					break;
				}
			}
			if (!hasContent) continue;

			// H 레코드
			codeList.add(String.format("H%-6s%06X%06X", progName, secStart, secLength));

			// D, R 레코드
			StringBuilder dRec = new StringBuilder("D");
			StringBuilder rRec = new StringBuilder("R");
			Set<String> extDefSet = new LinkedHashSet<>();
			Set<String> extRefSet = new LinkedHashSet<>();

			for (Token t : tt.getTokenList()) {
				if ("EXTDEF".equalsIgnoreCase(t.operator)) {
					for (String sym : t.operand) {
						extDefSet.add(sym);
						int addr = st.getSymbol(sym);
						dRec.append(String.format("%-6s%06X", sym, addr));
					}
				} else if ("EXTREF".equalsIgnoreCase(t.operator)) {
					for (String sym : t.operand) {
						extRefSet.add(sym);
						rRec.append(String.format("%-6s", sym));
					}
				}
			}
			if (dRec.length() > 1) codeList.add(dRec.toString());
			if (rRec.length() > 1) codeList.add(rRec.toString());

			// T/M 레코드 생성
			List<String> mRecs = new ArrayList<>();
			StringBuilder tBuffer = new StringBuilder();
			int tStart = -1, tLen = 0;

			for (Token t : tt.getTokenList()) {
				// LTORG 또는 END 만나면 리터럴 처리 및 flush
				if ("LTORG".equalsIgnoreCase(t.operator) || "END".equalsIgnoreCase(t.operator)) {
					if ("LTORG".equalsIgnoreCase(t.operator)) {
						if (tLen > 0) {
							codeList.add(String.format("T%06X%02X%s", tStart, tLen, tBuffer));
							tBuffer.setLength(0);
							tLen = 0;
						}
						// 2) litIdxs 에 모인 리터럴들을 각각 개행해서 찍기
						for (int idx : litIdxs) {
							byte[] data = literalToBytes(lt.getLiteral(idx));
							String obj   = bytesToHex(data);
							int  start   = lt.getLocation(idx) - secStart;
							codeList.add(String.format("T%06X%02X%s",
									start, data.length, obj));
						}
						litIdxs.clear();
						continue;
					}

					// opcode-only 버퍼가 남아있으면 flush
					StringBuilder full = new StringBuilder(tBuffer);
					int fullLen = tLen;
					for (int idx : litIdxs) {
						byte[] data = literalToBytes(lt.getLiteral(idx));
						full.append(bytesToHex(data));
						fullLen += data.length;
					}
					codeList.add(String.format("T%06X%02X%s", tStart, fullLen, full.toString()));

					// 버퍼 초기화
					tBuffer.setLength(0);
					tLen = 0;
					litIdxs.clear();

					if ("END".equalsIgnoreCase(t.operator)) break;
					continue;
				}

				if (!isTextRecordable(t)) continue;

				String objCode = generateObjectCode(t, t.location - secStart, st, lt, baseReg);
				if (objCode.isEmpty()) continue;

				int objLen = objCode.length()/2;

				// 새 T 레코드 시작
				if (tLen == 0) {
					tStart = t.location - secStart;
				}

				// 길이 초과하면 flush
				if (tLen + objLen > MAX_TEXT_RECORD_LENGTH) {
					codeList.add(String.format("T%06X%02X%s", tStart, tLen, tBuffer));
					tBuffer.setLength(0);
					tLen = 0;
					tStart = t.location - secStart;
				}

				tBuffer.append(objCode);
				tLen += objLen;

				// M 레코드 수집
				mRecs.addAll(generateModificationRecords(t, t.location - secStart, extDefSet, extRefSet));
			}

			// 루프 종료 후 버퍼 flush
			if (tLen > 0) {
				codeList.add(String.format("T%06X%02X%s", tStart, tLen, tBuffer));
				tBuffer.setLength(0);
				tLen = 0;
			}

			// 남은 리터럴들을 모두 T 레코드로 찍기
			for (int idx : litIdxs) {
				byte[] data = literalToBytes(lt.getLiteral(idx));
				String obj = bytesToHex(data);
				int start = lt.getLocation(idx) - secStart;
				codeList.add(String.format("T%06X%02X%s", start, data.length, obj));
			}
			litIdxs.clear();

			// M 레코드 출력
			for (String m : mRecs) {
				codeList.add(m);
			}


			// E 레코드
			if (sec == 0 && first.operand != null && first.operand.length > 0) {
				int entry = st.getSymbol(first.operand[0]);
				codeList.add(String.format("E%06X", secStart));
			} else {
				codeList.add("E");
			}
			codeList.add(""); // 섹션 구분
		}
	}

	// 헬퍼 1: literal -> byte[]
	private byte[] literalToBytes(String lit) {
		if (lit.startsWith("=C'")) {
			String chars = lit.substring(3, lit.length() - 1);
			return chars.getBytes(StandardCharsets.US_ASCII);
		} else if (lit.startsWith("=X'")) {
			String hex = lit.substring(3, lit.length() - 1);
			byte[] b = new byte[(hex.length() + 1) / 2];
			for (int i = 0; i < b.length; i++) {
				int start = 2 * i;
				int end = Math.min(start + 2, hex.length());
				b[i] = (byte) Integer.parseInt(hex.substring(start, end), 16);
			}
			return b;
		} else {
			// =12같은 10진수 리터럴은 3바이트 word로 저장
			int value = Integer.parseInt(lit.substring(1));
			byte[] b = new byte[3];
			b[0] = (byte) ((value >> 16) & 0xFF);
			b[1] = (byte) ((value >> 8)  & 0xFF);
			b[2] = (byte) ( value		 & 0xFF);
			return b;
		}
	}

	// 헬퍼 2: byte[] -> hex string
	private String bytesToHex(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for (byte bt : data) {
			sb.append(String.format("%02X", bt & 0xFF));
		}
		return sb.toString();
	}

	// 헬퍼 3: LTORG 케이스를 위한 emitLiteralRecords
	private void emitLiteralRecords(List<Integer> litIdxs, int secStart, LiteralTable lt) {
		for (int idx : litIdxs) {
			String lit = lt.getLiteral(idx);
			int loc = lt.getLocation(idx);
			byte[] data = literalToBytes(lit);
			String obj = bytesToHex(data);
			codeList.add(String.format("T%06X%02X%s", loc - secStart, data.length, obj));
		}
	}


	/**
	 * 작성된 codeList를 출력형태에 맞게 출력한다.<br>
	 * @param fileName : 저장되는 파일 이름
	 */
	private void printObjectCode(String fileName) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
		for (int i = 0; i < codeList.size(); i++) {
			bw.write(codeList.get(i));
			if (i < codeList.size() - 1) {
				bw.newLine();
			}
		}
		bw.close();
	}
}