import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * TokenTable: Pass1 단계에서 각 소스 라인을 Token 객체로 파싱하여 저장
 */
public class TokenTable {
	public static final int MAX_OPERAND = 3;
	// nixbpe flag bit values
	public static final int nFlag = 32;
	public static final int iFlag = 16;
	public static final int xFlag = 8;
	public static final int bFlag = 4;
	public static final int pFlag = 2;
	public static final int eFlag = 1;

	private SymbolTable symTab;
	private InstTable instTab;
	private ArrayList<Token> tokenList;

	/**
	 * 생성자: 심볼 테이블, 인스트럭션 테이블 링크 및 내부 리스트 초기화
	 */
	public TokenTable(SymbolTable symTab, InstTable instTab) {
		this.symTab = symTab;
		this.instTab = instTab;
		this.tokenList = new ArrayList<>();
	}

	/** 저장된 토큰 개수 반환 */
	public int size() {
		return tokenList.size();
	}

	/** 지정 인덱스의 Token 반환 */
	public Token getToken(int index) {
		return tokenList.get(index);
	}

	public ArrayList<Token> getTokenList() {
		return tokenList;
	}

	/**
	 * 입력 라인을 Token으로 파싱하여 리스트에 추가
	 * @param line 어셈블리 소스 한 줄
	 */
	public void putToken(String line) {
		tokenList.add(new Token(line, instTab));
	}

	/**
	 * pass2용 Object Code 생성
	 * @param index 토큰 인덱스
	 */
	public void makeObjectCode(int index) {
		Token t = getToken(index);
		// TODO: instTab, symTab 참조하여 t.objectCode 작성
	}

	/**
	 * 지정 인덱스의 Object Code 반환
	 */
	public String getObjectCode(int index) {
		return getToken(index).objectCode;
	}
}

/**
 * Token: 한 소스 라인을 label/operator/operand로 분해하여 저장
 */
class Token {
	int location;
	String label = "";
	String operator = "";
	String[] operand = new String[]{""};
	String comment = "";
	char nixbpe;
	String objectCode;
	int byteSize;

	/**
	 * 생성자: 한 줄을 파싱하여 필드 채움
	 * @param line 소스 코드 라인
	 * @param instTab 인스트럭션 명세 테이블
	 */
	public Token(String line, InstTable instTab) {
		this.operand = new String[TokenTable.MAX_OPERAND];
		Arrays.fill(this.operand, "");

		String trimmed = line.trim();
		if (trimmed.startsWith(".")) {
			this.comment = trimmed;
			return;
		}

		// 주석 분리
		int commentIndex = line.indexOf(".");
		if (commentIndex != -1) {
			comment = line.substring(commentIndex).trim();
			line = line.substring(0, commentIndex).trim();
		}

		String[] parts = line.split("\\s+");
		if (parts.length == 0) return;

		String first = parts[0];
		String key = first.startsWith("+") ? first.substring(1) : first;
		List<String> directives = Arrays.asList(
				"START","END","BYTE","WORD","RESW","RESB",
				"LTORG","CSECT","EXTDEF","EXTREF","EQU"
		);

		boolean isOp = instTab.instMap.containsKey(key) || directives.contains(key);
		int idx = 0;

		if (isOp) {
			this.operator = parts[0];
			idx = 1;
		} else {
			this.label = parts[0];
			if (parts.length > 1) {
				this.operator = parts[1];
			}
			idx = 2;
		}

		// operand 처리
		if (parts.length > idx) {
			String operandStr = parts[idx];
			// 콤마로 구분되는 경우 (최대 MAX_OPERAND개)
			String[] ops = operandStr.split(",");
			for (int i = 0; i < ops.length && i < TokenTable.MAX_OPERAND; i++) {
				this.operand[i] = ops[i].trim();
			}
		}
	}

	/**
	 * nixbpe flag 설정
	 */
	public void setFlag(int flag, int value) {
		if (value == 0) nixbpe &= ~flag;
		else nixbpe |= flag;
	}

	/**
	 * nixbpe flag 반환
	 */
	public int getFlag(int flags) {
		return nixbpe & flags;
	}
}