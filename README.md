Bitbucket Pull Request Builder Plugin
=====================================

This Jenkins plugin builds pull requests from Bitbucket.org and will report the test results.

### Current Maintainer(s):
***This repo is looking for a new maintainer.*** 

- [David Frascone](https://github.com/CodeMonk) - No longer has access to a jenkins system, due to a job change, and can no longer test any changes.

### Build Status

[![Build Status](https://travis-ci.org/nishio-dens/bitbucket-pullrequest-builder-plugin.svg?branch=master)](https://travis-ci.org/nishio-dens/bitbucket-pullrequest-builder-plugin)


Prerequisites
-------------

- Jenkins 1.625.3 or higher.
- https://wiki.jenkins-ci.org/display/JENKINS/Git+Plugin


Creating a Job
-------------

- Create a new job
- Select and configure Git SCM
	- Add Repository URL, `git@bitbucket.org:${repositoryOwner}/${repositoryName}.git`
	- In Branch Specifier, type `*/${sourceBranch}`
- Under Build Triggers, check Bitbucket Pull Request Builder
- In Cron, enter crontab for this job.
  - e.g. `* * * * *` will check for new pull requests every minute
- In Bitbucket BasicAuth Username, write your bitbucket username, like `jenkins@densan-labs.net`
- In Bitbucket BasicAuth Password, write your password
- In CI Identifier, enter an unique identifier among your Jenkins jobs related to the repo
- In CI Name, enter a human readable name for your Jenkins server
- Write RepositoryOwner
- Write RepositoryName
- Save to preserve your changes


Jenkins pipeline
-------------
```
pipeline {
    agent any
    triggers{
        bitbucketpr(projectPath:'',
            bitbucketServer:'<BITBUCKET_SERVER_URL>',
            cron: 'H/15 * * * *',
            credentialsId: '',
            username: '',
            password: '',
            repositoryOwner: '',
            repositoryName: '',
            branchesFilter: '',
            branchesFilterBySCMIncludes: false,
            ciKey: '',
            ciName: '',
            ciSkipPhrases: '',
            checkDestinationCommit: false,
            approveIfSuccess: false,
            cancelOutdatedJobs: true,
            buildChronologically: true,
            commentTrigger: '')
    }
}
```
Note that the `projectPath` parameter does not need to be set if `bitbucketServer`, `repositoryOwner`, and
`repositoryName` is set.  

You can use jenkins credentials by setting environment variables in the `environment` section
and referring to them like for example `"${env.BITBUCKET_CREDENTIALS_USR}"`.  

After you set up your Jenkins pipeline, run the job for the first time manually (otherwise the trigger may not work!)


Merge the Pull Request's Source Branch into the Target Branch Before Building
-----------------------------------------------------------------------------

You may want Jenkins to attempt to merge your PR before building.
This may help expose inconsistencies between the source branch and target branch.
Note that if the merge cannot be completed, the build will fail immediately.

- Follow the steps above in "Creating a Job"
- In the "Source Code Management" > "Git" > "Additional Behaviors" section, click "Add" > "Merge Before Building"
- In "Name of Repository" put "origin" (or, if not using default name, use your remote repository's name. Note: unlike in the main part of the Git Repository config, you cannot leave this item blank for "default").
- In "Branch to merge to" put "${targetBranch}" 
- Note that as long as you don't push these changes to your remote repository, the merge only happens in your local repository.

If you are merging into your target branch, you might want Jenkins to do a new build of the Pull Request when the target branch changes.
- There is a checkbox that says, "Rebuild if destination branch changes?" which enables this check.


Rerun a Build
-------------

If you want to rerun a pull request build, write a comment on your pull request reading “test this please”.


Environment Variables Provided
------------------------------

- `sourceBranch`
- `targetBranch`
- `repositoryOwner`
- `repositoryName`
- `pullRequestId`
- `destinationRepositoryOwner`
- `destinationRepositoryName`
- `pullRequestTitle`
- `pullRequestAuthor`


Contributing to Bitbucket Pull Request Builder Plugin
-----------------------------------------------------

- Do not use Fork [jenkinsci/bitbucket-pullrequest-builder-plugin](https://github.com/jenkinsci/bitbucket-pullrequest-builder-plugin) for contribution

- Use project [nishio-dens/bitbucket-pullrequest-builder-plugin](https://github.com/nishio-dens/bitbucket-pullrequest-builder-plugin)

- Check out the latest master to make sure the feature hasn't been implemented or the bug hasn't been fixed yet.

- Check out the issue tracker to make sure someone already hasn't requested it and/or contributed it.

- Fork the project.

- Start a feature/bugfix branch.

- Commit and push until you are happy with your contribution.



Donations
-----------------------------------------------------
Do you like this plugin? feel free to donate! 

Paypal: https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=LTXCF78GJ7224

BTC: 1KgwyVzefeNzJhuzqLq36E3bZi2WFjibMr

Thank you!

Copyright
---------

Copyright © 2022 S.nishio + Martin Damovsky + David Frascone


License
-------

- BSD License
- See COPYING file
