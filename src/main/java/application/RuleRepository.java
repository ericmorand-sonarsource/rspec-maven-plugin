package application;

import com.sonarsource.ruleapi.github.GitHubRuleMaker;
import domain.Rule;

import java.util.HashMap;
import java.util.List;

public class RuleRepository implements domain.RuleRepository {
    private final GitHubRuleMaker ruleMaker;

    public RuleRepository(GitHubRuleMaker ruleMaker) {
        this.ruleMaker = ruleMaker;
    }

    public List<Rule> getRulesByLanguage(String languageKey) {
        var ruleFiles = this.ruleMaker.getRulesByLanguage(languageKey, new HashMap<>(), file -> false);

        return ruleFiles.stream().map(ruleFile -> RuleFactory.create(
                languageKey,
                ruleFile
        )).toList();
    }
}
