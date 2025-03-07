package domain;

import java.util.List;

public interface RuleRepository {
    List<Rule> getRulesByLanguage(String languageKey);
}
