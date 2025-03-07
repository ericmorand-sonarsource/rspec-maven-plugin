package application;

import com.sonarsource.ruleapi.domain.RuleFiles;
import com.sonarsource.ruleapi.github.GitClone;
import com.sonarsource.ruleapi.github.GitHubRuleMaker;
import com.sonarsource.ruleapi.utilities.RuleApiCache;
import domain.Rule;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.util.List;

public class RuleRepository implements domain.RuleRepository {
    private final GitHubRuleMaker ruleMaker;

    public RuleRepository(String url, String branchName, Log logger) throws IOException {
        /**
         * Unfortunately, GitHubRuleMaker clones the repository during its creation.
         * Thus, we need to log as soon as we create it.
         */
        logger.info(String.format("Cloning repository %s, branch %s", url, branchName));

        var cacheKey = url
                .replaceAll(":", "_")
                .replaceAll("/", "_");


        logger.debug(String.format("Git repository cache key is %s", cacheKey));

        var gitClone = new GitClone(url, RuleApiCache.getCachePath().resolve(cacheKey), branchName);

        this.ruleMaker = new GitHubRuleMaker(gitClone);
    }

    public List<RuleFiles> getRuleManifestsByRuleSubdirectory(String ruleSubdirectory) {
        return this.ruleMaker.getRulesByRuleSubdirectory(ruleSubdirectory);
    }

    public List<Rule> getRulesByLanguage(String languageKey) {
        var ruleManifests = this.getRuleManifestsByRuleSubdirectory(languageKey);

        return ruleManifests.stream().map(ruleManifest -> RuleFactory.create(
                languageKey,
                ruleManifest
        )).toList();
    }
}
