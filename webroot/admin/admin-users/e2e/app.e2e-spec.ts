import { AdminHttpPage } from './app.po';

describe('admin-http App', function() {
  let page: AdminHttpPage;

  beforeEach(() => {
    page = new AdminHttpPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
