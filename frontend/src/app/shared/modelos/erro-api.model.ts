export interface ErroCampoApi {
  campo: string;
  mensagem: string;
}

export interface ErroApi {
  instante?: string;
  status: number;
  erro?: string;
  codigo: string;
  mensagem: string;
  caminho?: string;
  errosCampos?: ErroCampoApi[];
}