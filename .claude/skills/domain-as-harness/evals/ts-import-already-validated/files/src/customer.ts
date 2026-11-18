const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export class Customer {
  id?: number;
  name = "";
  email = "";

  isValid(): boolean {
    return this.name.trim().length > 0 && EMAIL.test(this.email);
  }
}
