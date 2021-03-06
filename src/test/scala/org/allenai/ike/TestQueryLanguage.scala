package org.allenai.ike

import org.allenai.common.testkit.UnitSpec

class TestQueryLanguage extends UnitSpec {

  "getQueryLength" should "get correct length" in {
    assertResult((2, 2)) {
      val query = QSeq(Seq(QWord(""), QNamed(QPos(""), "")))
      QueryLanguage.getQueryLength(query)
    }

    assertResult((1, -1)) {
      val query = QSeq(Seq(QStar(QWord("")), QNamed(QPos(""), "")))
      QueryLanguage.getQueryLength(query)
    }

    val disjLength2 = QDisj(Seq(QSeq(Seq(QWord(""), QPos(""))), QSeq(Seq(QWord(""), QPos("")))))
    val seqLength2 = QSeq(disjLength2.qexprs)

    assertResult((4, 4)) {
      val query = QSeq(Seq(disjLength2, disjLength2))
      QueryLanguage.getQueryLength(query)
    }

    assertResult((12, 12)) {
      val q1 = QSeq(Seq(seqLength2, seqLength2, QWord(""), disjLength2, QWildcard()))
      QueryLanguage.getQueryLength(q1)
    }

    assertResult((4, -1)) {
      val q1 = QUnnamed(QSeq(Seq(disjLength2, QSeq(Seq(QPos(""), QPlus(QWord("")))))))
      QueryLanguage.getQueryLength(q1)
    }

    assertResult((4, 4)) {
      val q1 = QueryLanguage.parse("(a b)[2,2]").get
      QueryLanguage.getQueryLength(q1)
    }

    assertResult((2, -1)) {
      val q1 = QueryLanguage.parse("(a (b c d)*)[2,2]").get
      QueryLanguage.getQueryLength(q1)
    }

    assertResult((2, 4)) {
      val q1 = QueryLanguage.parse("(a b)[1,2]").get
      QueryLanguage.getQueryLength(q1)
    }

    assertResult((1, 3)) {
      def w(): QWord = QWord("")
      val q1 = QSimilarPhrases(Seq(w(), w()), 2, Seq(
        SimilarPhrase(Seq(w(), w(), w()), 1),
        SimilarPhrase(Seq(w()), 1),
        SimilarPhrase(Seq(w(), w(), w(), w()), 1)
      ))
      QueryLanguage.getQueryLength(q1)
    }
  }

  "getQueryString" should "get correct string" in {
    def check(string: String) = {
      val qexpr = QueryLanguage.parse(string).get
      assert(QueryLanguage.getQueryString(qexpr) == string)
    }
    check("a b[1,2] (c d)* e")
    check("a {d,e}* e")
    check("((?:NP PP)*)")
    assert(QueryLanguage.getQueryString(QStar(QSeq(Seq(QWord("a"), QWord("b"))))) == "(?:a b)*")

  }
}
