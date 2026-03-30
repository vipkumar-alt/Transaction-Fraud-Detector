import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FraudCheckComponent } from './fraud-check';

describe('FraudCheckComponent', () => {
  let component: FraudCheckComponent;
  let fixture: ComponentFixture<FraudCheckComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FraudCheckComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(FraudCheckComponent);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
