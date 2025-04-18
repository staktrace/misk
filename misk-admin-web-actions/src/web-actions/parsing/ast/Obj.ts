import AstNode from '@web-actions/parsing/ast/AstNode';
import {
  MiskObjectType,
  MiskObjectTypes,
} from '@web-actions/api/responseTypes';

import Field from '@web-actions/parsing/ast/Field';
import Unexpected from '@web-actions/parsing/ast/Unexpected';
import { associateBy, expectFromRecord } from '@web-actions/utils/common';
import JsonValue from '@web-actions/parsing/ast/JsonValue';
import MiskType from '@web-actions/api/MiskType';

export default class Obj extends JsonValue {
  fields: Field[];
  type?: MiskObjectType;
  unexpected: Unexpected[];

  constructor(fields: Field[], unexpected: Unexpected[]) {
    super();
    this.fields = fields;
    for (const field of fields) {
      field.parent = this;
    }
    this.unexpected = unexpected;
  }

  childNodes(): AstNode[] {
    return [...this.fields, ...this.unexpected];
  }

  render(): string {
    return `{${this.fields.map((it) => it.render()).join(', ')}}`;
  }

  applyTypes(type: MiskType, types: MiskObjectTypes) {
    const objType = expectFromRecord(types, type.type);
    this.type = objType;
    const fieldDefinitions = associateBy(objType.fields, (it) => it.name);
    for (const field of this.fields) {
      const fieldDefinition = fieldDefinitions[field.name.value ?? ''];
      if (fieldDefinition) {
        field.applyTypes(fieldDefinition, types);
      }
    }
  }
}
