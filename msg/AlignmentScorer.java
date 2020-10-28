package msg;

import java.util.*;
import java.io.*;

class AlignmentScorer
{

/***
 * http://icb.med.cornell.edu/education/courses/introtobio/BLOSUM62
 *
 * Verbatim source is:
 *
 * # Entries for the BLOSUM62 matrix at a scale of ln(2)/2.0.
   A  R  N  D  C  Q  E  G  H  I  L  K  M  F  P  S  T  W  Y  V  B  J  Z  X  *
A  4 -1 -2 -2  0 -1 -1  0 -2 -1 -1 -1 -1 -2 -1  1  0 -3 -2  0 -2 -1 -1 -1 -4
R -1  5  0 -2 -3  1  0 -2  0 -3 -2  2 -1 -3 -2 -1 -1 -3 -2 -3 -1 -2  0 -1 -4
N -2  0  6  1 -3  0  0  0  1 -3 -3  0 -2 -3 -2  1  0 -4 -2 -3  4 -3  0 -1 -4
D -2 -2  1  6 -3  0  2 -1 -1 -3 -4 -1 -3 -3 -1  0 -1 -4 -3 -3  4 -3  1 -1 -4
C  0 -3 -3 -3  9 -3 -4 -3 -3 -1 -1 -3 -1 -2 -3 -1 -1 -2 -2 -1 -3 -1 -3 -1 -4
Q -1  1  0  0 -3  5  2 -2  0 -3 -2  1  0 -3 -1  0 -1 -2 -1 -2  0 -2  4 -1 -4
E -1  0  0  2 -4  2  5 -2  0 -3 -3  1 -2 -3 -1  0 -1 -3 -2 -2  1 -3  4 -1 -4
G  0 -2  0 -1 -3 -2 -2  6 -2 -4 -4 -2 -3 -3 -2  0 -2 -2 -3 -3 -1 -4 -2 -1 -4
H -2  0  1 -1 -3  0  0 -2  8 -3 -3 -1 -2 -1 -2 -1 -2 -2  2 -3  0 -3  0 -1 -4
I -1 -3 -3 -3 -1 -3 -3 -4 -3  4  2 -3  1  0 -3 -2 -1 -3 -1  3 -3  3 -3 -1 -4
L -1 -2 -3 -4 -1 -2 -3 -4 -3  2  4 -2  2  0 -3 -2 -1 -2 -1  1 -4  3 -3 -1 -4
K -1  2  0 -1 -3  1  1 -2 -1 -3 -2  5 -1 -3 -1  0 -1 -3 -2 -2  0 -3  1 -1 -4
M -1 -1 -2 -3 -1  0 -2 -3 -2  1  2 -1  5  0 -2 -1 -1 -1 -1  1 -3  2 -1 -1 -4
F -2 -3 -3 -3 -2 -3 -3 -3 -1  0  0 -3  0  6 -4 -2 -2  1  3 -1 -3  0 -3 -1 -4
P -1 -2 -2 -1 -3 -1 -1 -2 -2 -3 -3 -1 -2 -4  7 -1 -1 -4 -3 -2 -2 -3 -1 -1 -4
S  1 -1  1  0 -1  0  0  0 -1 -2 -2  0 -1 -2 -1  4  1 -3 -2 -2  0 -2  0 -1 -4
T  0 -1  0 -1 -1 -1 -1 -2 -2 -1 -1 -1 -1 -2 -1  1  5 -2 -2  0 -1 -1 -1 -1 -4
W -3 -3 -4 -4 -2 -2 -3 -2 -2 -3 -2 -3 -1  1 -4 -3 -2 11  2 -3 -4 -2 -2 -1 -4
Y -2 -2 -2 -3 -2 -1 -2 -3  2 -1 -1 -2 -1  3 -3 -2 -2  2  7 -1 -3 -1 -2 -1 -4
V  0 -3 -3 -3 -1 -2 -2 -3 -3  3  1 -2  1 -1 -2 -2  0 -3 -1  4 -3  2 -2 -1 -4
B -2 -1  4  4 -3  0  1 -1  0 -3 -4  0 -3 -3 -2  0 -1 -4 -3 -3  4 -3  0 -1 -4
J -1 -2 -3 -3 -1 -2 -3 -4 -3  3  3 -3  2  0 -3 -2 -1 -2 -1  2 -3  3 -3 -1 -4
Z -1  0  0  1 -3  4  4 -2  0 -3 -3  1 -1 -3 -1  0 -1 -2 -2 -2  0 -3  4 -1 -4
X -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -4
* -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4  1

*/

    private final static int[][]        BLOSUM_62_MATRIX;
    private final static String         INDICES = "ARNDCQEGHILKMFPSTWYVBJZX";
    private final static String[]       RAW_ROWS =
    {
        "A  4 -1 -2 -2  0 -1 -1  0 -2 -1 -1 -1 -1 -2 -1  1  0 -3 -2  0 -2 -1 -1 -1 -4",
        "R -1  5  0 -2 -3  1  0 -2  0 -3 -2  2 -1 -3 -2 -1 -1 -3 -2 -3 -1 -2  0 -1 -4",
        "N -2  0  6  1 -3  0  0  0  1 -3 -3  0 -2 -3 -2  1  0 -4 -2 -3  4 -3  0 -1 -4",
        "D -2 -2  1  6 -3  0  2 -1 -1 -3 -4 -1 -3 -3 -1  0 -1 -4 -3 -3  4 -3  1 -1 -4",
        "C  0 -3 -3 -3  9 -3 -4 -3 -3 -1 -1 -3 -1 -2 -3 -1 -1 -2 -2 -1 -3 -1 -3 -1 -4",
        "Q -1  1  0  0 -3  5  2 -2  0 -3 -2  1  0 -3 -1  0 -1 -2 -1 -2  0 -2  4 -1 -4",
        "E -1  0  0  2 -4  2  5 -2  0 -3 -3  1 -2 -3 -1  0 -1 -3 -2 -2  1 -3  4 -1 -4",
        "G  0 -2  0 -1 -3 -2 -2  6 -2 -4 -4 -2 -3 -3 -2  0 -2 -2 -3 -3 -1 -4 -2 -1 -4",
        "H -2  0  1 -1 -3  0  0 -2  8 -3 -3 -1 -2 -1 -2 -1 -2 -2  2 -3  0 -3  0 -1 -4",
        "I -1 -3 -3 -3 -1 -3 -3 -4 -3  4  2 -3  1  0 -3 -2 -1 -3 -1  3 -3  3 -3 -1 -4",
        "L -1 -2 -3 -4 -1 -2 -3 -4 -3  2  4 -2  2  0 -3 -2 -1 -2 -1  1 -4  3 -3 -1 -4",
        "K -1  2  0 -1 -3  1  1 -2 -1 -3 -2  5 -1 -3 -1  0 -1 -3 -2 -2  0 -3  1 -1 -4",
        "M -1 -1 -2 -3 -1  0 -2 -3 -2  1  2 -1  5  0 -2 -1 -1 -1 -1  1 -3  2 -1 -1 -4",
        "F -2 -3 -3 -3 -2 -3 -3 -3 -1  0  0 -3  0  6 -4 -2 -2  1  3 -1 -3  0 -3 -1 -4",
        "P -1 -2 -2 -1 -3 -1 -1 -2 -2 -3 -3 -1 -2 -4  7 -1 -1 -4 -3 -2 -2 -3 -1 -1 -4",
        "S  1 -1  1  0 -1  0  0  0 -1 -2 -2  0 -1 -2 -1  4  1 -3 -2 -2  0 -2  0 -1 -4",
        "T  0 -1  0 -1 -1 -1 -1 -2 -2 -1 -1 -1 -1 -2 -1  1  5 -2 -2  0 -1 -1 -1 -1 -4",
        "W -3 -3 -4 -4 -2 -2 -3 -2 -2 -3 -2 -3 -1  1 -4 -3 -2 11  2 -3 -4 -2 -2 -1 -4",
        "Y -2 -2 -2 -3 -2 -1 -2 -3  2 -1 -1 -2 -1  3 -3 -2 -2  2  7 -1 -3 -1 -2 -1 -4",
        "V  0 -3 -3 -3 -1 -2 -2 -3 -3  3  1 -2  1 -1 -2 -2  0 -3 -1  4 -3  2 -2 -1 -4",
        "B -2 -1  4  4 -3  0  1 -1  0 -3 -4  0 -3 -3 -2  0 -1 -4 -3 -3  4 -3  0 -1 -4",
        "J -1 -2 -3 -3 -1 -2 -3 -4 -3  3  3 -3  2  0 -3 -2 -1 -2 -1  2 -3  3 -3 -1 -4",
        "Z -1  0  0  1 -3  4  4 -2  0 -3 -3  1 -1 -3 -1  0 -1 -2 -2 -2  0 -3  4 -1 -4",
        "X -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -1 -4",
        "* -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4 -4  1"
    };


    static boolean isAa(char ch)
    {
        ch = Character.toUpperCase(ch);
        if (ch < 'A'  ||  ch > 'Z')
            return false;
        return "BJOUXZ".indexOf(ch) < 0;
    }


    static
    {
        // Convert raw string table to a map of maps.
        TreeMap<Character, TreeMap<Character, Integer>> mapOfMaps =
            new TreeMap<Character, TreeMap<Character, Integer>>();
        for (String row: RAW_ROWS)
        {
            String[] rawPieces = row.split(" ");     // produces some length=0 pieces
            Vector<String> pieces = new Vector<String>();
            for (String rawPiece: rawPieces)
                if (rawPiece.trim().length() > 0)
                    pieces.add(rawPiece.trim());
            assert pieces.size() == 26 : pieces.size();
            // (0) = amino acid, (1-25) = vals by INDICES, including 5 weirdos
            // at the end for B/J/Z/X/*.
            Character aa = pieces.remove(0).charAt(0);
            TreeMap<Character, Integer> map = new TreeMap<Character, Integer>();
            mapOfMaps.put(aa, map);
            for (int i=0; i<20; i++)
            {
                Character otherAa = INDICES.charAt(i);
                Integer val = Integer.parseInt(pieces.get(i));
                map.put(otherAa, val);
            }
        }

        // Convert map of maps to 26x26 array. Only entries for the 20 amino
        // acids are valid.
        BLOSUM_62_MATRIX = new int[26][26];
        for (int i=0; i<26; i++)
            for (int j=0; j<26; j++)
                BLOSUM_62_MATRIX[i][j] = Integer.MIN_VALUE;
        String notAas = "BJOUXZ";
        for (char aa: mapOfMaps.keySet())
        {
            if (!isAa(aa))
                continue;
            Map<Character, Integer> map = mapOfMaps.get(aa);
            for (char otherAa: map.keySet())
            {
                if (!isAa(otherAa))
                    continue;
                int val = map.get(otherAa);
                BLOSUM_62_MATRIX[aa-'A'][otherAa-'A'] = val;
            }
        }

        // Misc checks.
        for (char aa='A'; aa<= 'Z'; aa++)
        {
            if (!isAa(aa))
                continue;
            for (char aa1='A'; aa1<= 'Z'; aa1++)
            {
                if (!isAa(aa1))
                    continue;
                assert BLOSUM_62_MATRIX[aa-'A'][aa1-'A'] ==
                       BLOSUM_62_MATRIX[aa1-'A'][aa-'A'];
                assert BLOSUM_62_MATRIX[aa-'A'][aa1-'A'] > Integer.MIN_VALUE;
            }
        }
        assert BLOSUM_62_MATRIX['C'-'A']['Q'-'A'] == -3;    // Spot checks
        assert BLOSUM_62_MATRIX['M'-'A']['M'-'A'] == 5;
        assert BLOSUM_62_MATRIX['A'-'A']['P'-'A'] == -1;
    }


    static int scoreAlignment(Collection<String> alignment)
    {
        // Convert input to char[][].
        char[][] charAlignment = new char[alignment.size()][];
        int n = 0;
        for (String s: alignment)
            charAlignment[n++] = s.toCharArray();

        // Score.
        return scoreAlignment(charAlignment);
    }


    // More efficient, since scoreAlignment(Collection<String>) converts to
    // this format.
    static int scoreAlignment(char[][] alignment)
    {
        // Collect all-gap columns.
        int w = alignment[0].length;
        boolean[] ungapped = new boolean[w];
        for (int seq=0; seq<alignment.length; seq++)
        {
            for (int col=0; col<alignment[0].length; col++)
            {
                if (alignment[seq][col] != '-')
                {
                    ungapped[col] = true;
                    break;
                }
            }
        }

        int score = 0;
        for (int col=0; col<alignment[0].length; col++)
            score += scoreColumn(alignment, col);
        for (int row=0; row<alignment.length; row++)
            score += gapPenaltiesForRow(alignment[row], ungapped);
        return score;
    }


    private static int scoreColumn(char[][] alignment, int col)
    {
        int score = 0;

        try
        {
            for (int row1=0; row1<alignment.length-1; row1++)
            {
                char ch1 = alignment[row1][col];
                if (ch1 == '-')
                    continue;
                for (int row2=row1+1; row2<alignment.length; row2++)
                {
                    char ch2 = alignment[row2][col];
                    if (ch2 == '-')
                        continue;
                    int partialScore = BLOSUM_62_MATRIX[ch1-'A'][ch2-'A'];
                    assert partialScore >= -100  &&  partialScore <= 100  :
                           "bad chars [" + ch1 + "][" + ch2 + "]";
                    score += BLOSUM_62_MATRIX[ch1-'A'][ch2-'A'];
                }
            }
        }
        catch (Exception x)
        {
            sop("?????");
        }

        return score;
    }


    // Gap-open penalty = 11, gap-extend penalty = 1. Only look at columns
    // that aren't completely gaps (ungappedCols[n] = true).
    private static int gapPenaltiesForRow(char[] row, boolean[] ungappedCols)
    {
        int score = 0;
        char prevChar = '#';
        for (int col=0; col<row.length; col++)
        {
            if (!ungappedCols[col])
                continue;
            char currentChar = row[col];
            if (currentChar == '-')
            {
                if (prevChar == '-')
                    score -= 1;     // extend
                else
                    score -= 11;    // open
            }
            prevChar = currentChar;
        }
        return score;
    }


    static void sop(Object x)               { System.out.println(x); }


    public static void main(String[] args)
    {
        sop("21^30 = " + Math.pow(21, 30));
        char[] chars = {'E', 'V'};
        for (int i=0; i<2; i++)
        {
            for (int j=0; j<2; j++)
            {
                char c1 = chars[i];
                char c2 = chars[j];
                int score = BLOSUM_62_MATRIX[c1-'A'][c2-'A'];
                sop(c1 + "->" + c2 + " = " + score);
            }
        }
    }
}
