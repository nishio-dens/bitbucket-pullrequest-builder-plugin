Bitbucket Pull Request Builder Plugin
================================

This Jenkins plugin builds pull requests from Bitbucket.org and will report the test results.


Prerequisites
================================

- Jenkins 1.509.4 or higher.
- https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin


Creating a Job
=================================

- Create a new job
- Select Git SCM
- Add Repository URL as bellow
  - git@bitbucket.org:${repositoryOwner}/${repositoryName}.git
- In Branch Specifier, type as bellow
  - */${sourceBranch}
- Under Build Triggers, check Bitbucket Pull Request Builder
- In Cron, enter crontab for this job.
  - ex: * * * * *
- In Bitbucket BasicAuth Username, write your bitbucket username like jenkins@densan-labs.net
- In Bitbucket BasicAuth Password, write your password
- In CI Identifier, enter an unique identifier among your Jenkins jobs related to the repo
- In CI Name, enter a human readable name for your Jenkins server
- Write RepositoryOwner
- Write RepositoryName
- Save to preserve your changes

Merge the Pull Request's Source Branch into the Target Branch Before Building
==============================================================================
You may want Jenkins to attempt to merge your PR before doing the build -- this way it will find conflicts for you automatically.
- Follow the steps above in "Creating a Job"
- In the "Source Code Management" > "Git" > "Additional Behaviors" section, click "Add" > "Merge Before Building"
- In "Name of Repository" put "origin" (or, if not using default name, use your remote repository's name. Note: unlike in the main part of the Git Repository config, you cannot leave this item blank for "default".)
- In "Branch to merge to" put "${targetBranch}" 
- Note that as long as you don't push these changes to your remote repository, the merge only happens in your local repository.


If you are merging into your target branch, you might want Jenkins to do a new build of the Pull Request when the target branch changes.
- There is a checkbox that says, "Rebuild if destination branch changes?" which enables this check.


Rerun test builds
====================

If you want to rerun pull request test, write “test this please” comment to your pull request.




Contributing to Bitbucket Pull Request Builder Plugin
================================================

- Check out the latest master to make sure the feature hasn't been implemented or the bug hasn't been fixed yet.

- Check out the issue tracker to make sure someone already hasn't requested it and/or contributed it.

- Fork the project.

- Start a feature/bugfix branch.

- Commit and push until you are happy with your contribution.


Copyright
=============================

Copyright © 2014 S.nishio. 


License
=============================

- BSD License
- See COPYING file
