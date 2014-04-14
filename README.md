Bitbucket Pull Request Builder Plugin
================================

This Jenkins plugin builds pull requests from Bitbucket.org and will report the test results.


Prerequisites
================================

- Jenkins 1.509.4 or higher.
- https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin

Installation
================================

- Download this plugin from http://doc.densan-labs.net/bitbucket-pullrequest-builder.hpi (Temporary link)
- Go to Jenkins -> Manage Plugins -> Advanced 
- Choose File and select .hpi file above and upload
- Ensure you restart Jenkins


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
- Write RepositoryOwner
- Write RepositoryName
- Save to preserve your changes


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
