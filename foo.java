class foo
{
    public static void main(String[] args) 
    {
        int i = 27;
        int j = 38;
        int k = Integer.parseInt(args [0]);

        i = j ^ k;
        j = i / 37;
        
        System.out.println("hello world" + i + j + j);
    }
}
