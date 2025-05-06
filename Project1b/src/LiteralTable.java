import java.util.ArrayList;

public class LiteralTable {
    private ArrayList<String> literalList;
    private ArrayList<Integer> locationList;

    // 생성자: 내부 리스트 초기화
    public LiteralTable() {
        this.literalList  = new ArrayList<>();
        this.locationList = new ArrayList<>();
    }

    // 리터럴 추가
    public void putLiteral(String literal) {
        literalList.add(literal);
        locationList.add(-1);    // ★ 여기서 기본값으로 -1을 등록
    }

    /**
     * 리터럴 테이블 크기 반환
     * @return 등록된 리터럴 수
     */
    public int size() {
        return literalList.size();
    }

    /**
     * 인덱스에 해당하는 리터럴 반환
     */
    public String getLiteral(int index) {
        return literalList.get(index);
    }

    /**
     * 인덱스에 해당하는 리터럴의 주소를 설정
     * @param index   리터럴 인덱스
     * @param address 할당할 주소
     */
    public void setLocation(int index, int address) {
        locationList.set(index, address);
    }

    /**
     * 인덱스에 해당하는 리터럴 주소 반환
     */
    public int getLocation(int index) {
        return locationList.get(index);
    }

    // 포맷 출력
    @Override
    public String toString() {
        if (literalList == null || literalList.isEmpty() || locationList == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int count = Math.min(literalList.size(), locationList.size());
        for (int i = 0; i < count; i++) {
            sb.append(String.format("%-10s %X", literalList.get(i), locationList.get(i)));
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
