function DatasetsPage(browser) {
  this.browser = browser;
}

var CLICK_RESULT_STATUS_FAULURE = -1;


var LOCATORS = {
  body: 'body',
  featuredDatasetCreateBtn: '.featured-dataset-col button:nth-of-type(1)', // just select the first featured dataset
  xPathFeaturedDatasetCreateBtn: '//button[@type="button" and contains(., "Create project")]',
  projectNameInput: 'form[ng-submit="createProject(project)"] input[ng-model="project.name"]',
  projectDescriptionTxtFld: 'form[ng-submit="createProject(project)"] textarea[ng-model="project.description"]',
  createProjectModelCreateBtn: 'form[ng-submit="createProject(project)"] button[type="submit"]',
  projectTab: 'dd[heading="Projects"]'
};

DatasetsPage.prototype = {
  waitForPageToLoad: function() {
    this.browser.waitForElementVisible(LOCATORS.body, 50000);
  },
  end: function() {
    this.browser.end();
  },
  selectProjectsTab: function() {
    this.browser
      .waitForElementVisible(LOCATORS.projectTab, 15000)
      .click(LOCATORS.projectTab);
  },
  createFeaturedDatasetProject: function(name, desc) {
    this.browser
      .useXpath()
      .waitForElementVisible(LOCATORS.xPathFeaturedDatasetCreateBtn, 15000)
      .click(LOCATORS.xPathFeaturedDatasetCreateBtn, function(res) {
        console.log('create project btn clicked ');
        if (res && CLICK_RESULT_STATUS_FAULURE === res.status) {
          console.error(res);
        }
      })
      .useCss()
      .pause(300)
      .clearValue(LOCATORS.projectNameInput)
      .setValue(LOCATORS.projectNameInput, name)
      .clearValue(LOCATORS.projectDescriptionTxtFld)
      .setValue(LOCATORS.projectDescriptionTxtFld, desc)
      .pause(600)
      .click(LOCATORS.createProjectModelCreateBtn).pause(300);
  }
};

module.exports = DatasetsPage;
