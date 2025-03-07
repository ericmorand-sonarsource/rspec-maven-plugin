package application;

import domain.*;

import domain.Exception;
import infrastructure.JVMHost;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;

@Mojo(name = "generate-registrars")
public class GenerateRegistrarsMojo extends AbstractMojo {
    @Parameter(property = "rspec.languageKey", required = true)
    private String languageKey;

    @Parameter(property = "rspec.targetDirectory", required = true)
    private String targetDirectory;

    @Parameter(property = "rspec.vcsRepositoryUrl", defaultValue = "https://github.com/SonarSource/rspec.git")
    private String vcsRepositoryUrl;

    @Parameter(property = "rspec.vcsBranchName", defaultValue = "master")
    private String vcsBranchName;

    @Parameter(property = "rspec.profileName", defaultValue = "Sonar way")
    private String profileName;

    @Parameter(property = "rspec.packageName", required = true)
    private String packageName;

    // todo: should probably not be required and default to languageKey
    @Parameter(property = "rspec.compatibleLanguageKey", required = true)
    private String compatibleLanguageKey;

    // todo: should probably not be required and default to languageKey
    @Parameter(property = "rspec.repositoryKey", required = true)
    private String repositoryKey;

    @Override
    public void execute() throws MojoExecutionException {
        var host = new JVMHost();
        var logger = this.getLog();

        try {
            var generator = new RegistrarsGenerator(
                    logger::info,
                    new RuleRepository(this.vcsRepositoryUrl, this.vcsBranchName, logger),
                    new FileSystem(host)
            );

            generator.execute(this.packageName, this.languageKey, this.compatibleLanguageKey, this.repositoryKey, this.targetDirectory, this.profileName);
        } catch (Exception | IOException e) {
            throw new MojoExecutionException(e);
        }
    }
}
